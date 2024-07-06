/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.vanilla.installer;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import net.minecraftforge.fart.api.Renamer;
import net.minecraftforge.fart.api.SignatureStripperConfig;
import net.minecraftforge.fart.api.SourceFixerConfig;
import net.minecraftforge.fart.api.Transformer;
import net.minecraftforge.srgutils.IMappingFile;
import org.spongepowered.vanilla.installer.model.mojang.Version;
import org.spongepowered.vanilla.installer.model.mojang.VersionManifest;
import org.tinylog.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

public final class InstallerMain {

    private static final String COLLECTION_BOOTSTRAP = "bootstrap"; // goes on app
    private static final String COLLECTION_MAIN = "main"; // goes on TCL
    private static final int MAX_TRIES = 2;

    private final Installer installer;

    public InstallerMain(final String[] args) throws Exception {
        LauncherCommandLine.configure(args);
        this.installer = new Installer(LauncherCommandLine.installerDirectory);
    }

    public static void main(final String[] args) throws Exception {
        new InstallerMain(args).run();
    }

    public void run() {
        try  {
            this.downloadAndRun();
        } catch (final Exception ex) {
            Logger.error(ex, "Failed to download Sponge libraries and/or Minecraft");
            System.exit(2);
        } finally {
            this.installer.getLibraryManager().finishedProcessing();
        }
    }

    public void downloadAndRun() throws Exception {
        Path remappedMinecraftJar = null;
        Version mcVersion = null;
        try {
            mcVersion = this.downloadMinecraftManifest();
        } catch (final IOException ex) {
            remappedMinecraftJar = this.recoverFromMinecraftDownloadError(ex);
            this.installer.getLibraryManager().validate();
        }

        try {
            if (mcVersion != null) {
                final CompletableFuture<Path> mappingsFuture = this.downloadMappings(mcVersion, LauncherCommandLine.librariesDirectory);
                final CompletableFuture<Path> originalMcFuture = this.downloadMinecraft(mcVersion, LauncherCommandLine.librariesDirectory);
                final CompletableFuture<Path> remappedMinecraftJarFuture = mappingsFuture.thenCombineAsync(originalMcFuture, (mappings, minecraft) -> {
                    try {
                        return this.remapMinecraft(minecraft, mappings, this.installer.getLibraryManager().preparationWorker());
                    } catch (final IOException ex) {
                        return AsyncUtils.sneakyThrow(ex);
                    }
                }, this.installer.getLibraryManager().preparationWorker());
                this.installer.getLibraryManager().validate();
                remappedMinecraftJar = remappedMinecraftJarFuture.get();
            }
        } catch (final ExecutionException ex) {
            final /* @Nullable */ Throwable cause = ex.getCause();
            remappedMinecraftJar = this.recoverFromMinecraftDownloadError(cause instanceof Exception ? (Exception) cause : ex);
        }
        assert remappedMinecraftJar != null; // always assigned or thrown

        // We need MC at the bootstrap level, since the server jar has some of its dependencies bundled
        // Changes in 1.18 will allow us to easily isolate the server itself from its dependencies.
        this.installer.getLibraryManager().addLibrary(InstallerMain.COLLECTION_BOOTSTRAP, new LibraryManager.Library("minecraft", remappedMinecraftJar));
        this.installer.getLibraryManager().finishedProcessing();

        Logger.info("Environment has been verified.");

        this.installer.getLibraryManager().getAll(InstallerMain.COLLECTION_BOOTSTRAP).stream()
            .map(LibraryManager.Library::getFile)
            .forEach(path -> {
                Logger.debug("Adding jar {} to classpath", path);
                Agent.addJarToClasspath(path);
            });

        final Path[] transformableLibs = this.installer.getLibraryManager().getAll(COLLECTION_MAIN).stream()
            .map(LibraryManager.Library::getFile)
            .toArray(Path[]::new);

        final List<String> gameArgs = new ArrayList<>(LauncherCommandLine.remainingArgs);
        Collections.addAll(gameArgs, this.installer.getLauncherConfig().args.split(" "));

        // Suppress illegal reflection warnings on newer java
        Agent.crackModules();

        final String className = "org.spongepowered.vanilla.applaunch.Main";
        InstallerMain.invokeMain(className, gameArgs.toArray(new String[0]), transformableLibs);
    }

    private <T extends Throwable> Path recoverFromMinecraftDownloadError(final T ex) throws T {
        final Path expectedMinecraftJar = this.expectedRemappedLocation(this.expectedMinecraftLocation(LauncherCommandLine.librariesDirectory, Constants.Libraries.MINECRAFT_VERSION_TARGET));
        if (Files.exists(expectedMinecraftJar)) {
            Logger.warn(ex, "Failed to download and remap Minecraft. An existing jar exists, so we will attempt to use that instead.");
            return expectedMinecraftJar;
        } else {
            throw ex;
        }
    }

    private static void invokeMain(final String className, final String[] args, final Path[] extraCpEntries) {
        try {
            Class.forName(className)
                .getMethod("main", String[].class, Path[].class)
                .invoke(null, args, extraCpEntries);
        } catch (final InvocationTargetException ex) {
            Logger.error(ex.getCause(), "Failed to invoke main class {} due to an error", className);
            System.exit(1);
        } catch (final ClassNotFoundException | NoSuchMethodException | IllegalAccessException ex) {
            Logger.error(ex, "Failed to invoke main class {} due to an error", className);
            System.exit(1);
        }
    }

    private Version downloadMinecraftManifest() throws IOException {
        Logger.info("Downloading the Minecraft versions manifest...");

        VersionManifest.Version foundVersionManifest = null;

        final Gson gson = new Gson();
        final URLConnection conn = new URL(Constants.Libraries.MINECRAFT_MANIFEST_URL)
            .openConnection();
        conn.setConnectTimeout(5 /* seconds */ * 1000);
        try (final JsonReader reader = new JsonReader(new InputStreamReader(conn.getInputStream()))) {
            final VersionManifest manifest = gson.fromJson(reader, VersionManifest.class);
            for (final VersionManifest.Version version : manifest.versions) {
                if (Constants.Libraries.MINECRAFT_VERSION_TARGET.equals(version.id)) {
                    foundVersionManifest = version;
                    break;
                }
            }
        }

        if (foundVersionManifest == null) {
            throw new IOException(String.format("Failed to find version manifest for '%s'!", Constants.Libraries.MINECRAFT_VERSION_TARGET));
        }

        final Version version;

        try (final JsonReader reader = new JsonReader(new InputStreamReader(foundVersionManifest.url.openStream()))) {
            version = gson.fromJson(reader, Version.class);
        }

        if (version == null) {
            throw new IOException(String.format("Failed to download version information for '%s'!",
                    Constants.Libraries.MINECRAFT_VERSION_TARGET));
        }

        return version;
    }

    private Path expectedMinecraftLocation(final Path librariesDirectory, final String version) {
        return librariesDirectory.resolve(Constants.Libraries.MINECRAFT_PATH_PREFIX)
            .resolve(version)
            .resolve(Constants.Libraries.MINECRAFT_SERVER_JAR_NAME + ".jar");
    }

    private Path expectedRemappedLocation(final Path originalLocation) {
        return originalLocation.resolveSibling(Constants.Libraries.MINECRAFT_SERVER_JAR_NAME + "_remapped.jar");
    }

    private CompletableFuture<Path> downloadMinecraft(final Version version, final Path librariesDirectory) {
        return AsyncUtils.asyncFailableFuture(() -> {
            final Path downloadTarget = this.expectedMinecraftLocation(librariesDirectory, version.id);

            if (Files.notExists(downloadTarget)) {
                if (!this.installer.getLauncherConfig().autoDownloadLibraries) {
                    throw new IOException(
                            String.format("The Minecraft jar is not located at '%s' and downloading it has been turned off.", downloadTarget));
                }
                InstallerUtils
                        .downloadCheckHash(version.downloads.server.url, downloadTarget, MessageDigest.getInstance("SHA-1"),
                                version.downloads.server.sha1, false);
            } else {
                if (this.installer.getLauncherConfig().checkLibraryHashes) {
                    Logger.info("Detected existing Minecraft Server jar, verifying hashes...");

                    // Pipe the download stream into the file and compute the SHA-1
                    if (InstallerUtils.validateSha1(version.downloads.server.sha1, downloadTarget)) {
                        Logger.info("Minecraft Server jar verified!");
                    } else {
                        Logger.error("Checksum verification failed: Expected {}. Deleting cached Minecraft Server jar...",
                                version.downloads.server.sha1);
                        Files.delete(downloadTarget);
                        InstallerUtils.downloadCheckHash(version.downloads.server.url, downloadTarget,
                                MessageDigest.getInstance("SHA-1"), version.downloads.server.sha1, false);
                    }
                } else {
                    Logger.info("Detected existing Minecraft jar. Skipping hash check as that is turned off...");
                }
            }
            return downloadTarget;
        }, this.installer.getLibraryManager().preparationWorker());
    }

    private CompletableFuture<Path> downloadMappings(final Version version, final Path librariesDirectory) {
        return AsyncUtils.asyncFailableFuture(() -> {
            Logger.info("Setting up names for Minecraft {}", Constants.Libraries.MINECRAFT_VERSION_TARGET);
            final Path downloadTarget = librariesDirectory.resolve(Constants.Libraries.MINECRAFT_MAPPINGS_PREFIX)
                    .resolve(Constants.Libraries.MINECRAFT_VERSION_TARGET)
                    .resolve(Constants.Libraries.MINECRAFT_MAPPINGS_NAME);

            final Version.Downloads.Download mappings = version.downloads.server_mappings;
            if (mappings == null) {
                throw new IOException(String.format("Mappings were not included in version manifest for %s", Constants.Libraries.MINECRAFT_VERSION_TARGET));
            }

            final boolean checkHashes = this.installer.getLauncherConfig().checkLibraryHashes;
            if (Files.exists(downloadTarget)) {
                if (checkHashes) {
                    Logger.info("Detected existing mappings, verifying hashes...");
                    if (InstallerUtils.validateSha1(mappings.sha1, downloadTarget)) {
                        Logger.info("Mappings verified!");
                        return downloadTarget;
                    } else {
                        Logger.error("Checksum verification failed: Expected {}. Deleting cached server mappings file...",
                            version.downloads.server.sha1);
                        Files.delete(downloadTarget);
                    }
                } else {
                    return downloadTarget;
                }
            }

            if (this.installer.getLauncherConfig().autoDownloadLibraries) {
                if (checkHashes) {
                    InstallerUtils.downloadCheckHash(mappings.url, downloadTarget,
                        MessageDigest.getInstance("SHA-1"), mappings.sha1, false);
                } else {
                    InstallerUtils.download(mappings.url, downloadTarget, false);
                }
            } else {
                throw new IOException(String.format("Mappings were not located at '%s' and downloading them has been turned off.", downloadTarget));
            }

            return downloadTarget;
        }, this.installer.getLibraryManager().preparationWorker());
    }

    private Path remapMinecraft(final Path inputJar, final Path serverMappings, final ExecutorService service) throws IOException {
        Logger.info("Checking if we need to remap Minecraft...");
        final Path outputJar = this.expectedRemappedLocation(inputJar);
        final Path tempOutput = outputJar.resolveSibling(Constants.Libraries.MINECRAFT_SERVER_JAR_NAME + "_remapped.jar.tmp");

        if (Files.exists(outputJar)) {
            Logger.info("Remapped Minecraft detected, skipping...");
            return outputJar;
        }

        Logger.info("Remapping Minecraft. This may take a while...");
        final IMappingFile mappings = IMappingFile.load(serverMappings.toFile()).reverse();

        Renamer.builder()
            .input(inputJar.toFile())
            .output(tempOutput.toFile())
            .add(Transformer.parameterAnnotationFixerFactory())
            .add(ctx -> {
              final Transformer backing = Transformer.renamerFactory(mappings).create(ctx);
              return new Transformer() {

                @Override
                public ClassEntry process(final ClassEntry entry) {
                    final String name = entry.getName();
                    if (name.startsWith("it/unimi")
                        || name.startsWith("com/google")
                        || name.startsWith("com/mojang/datafixers")
                        || name.startsWith("com/mojang/brigadier")
                        || name.startsWith("org/apache")) {
                        return entry;
                    }
                    return backing.process(entry);
                }

                @Override
                public ManifestEntry process(final ManifestEntry entry) {
                    return backing.process(entry);
                }

                @Override
                public ResourceEntry process(final ResourceEntry entry) {
                    return backing.process(entry);
                }

                @Override
                public Collection<? extends Entry> getExtras() {
                    return backing.getExtras();
                }

              };
            })
            .add(Transformer.recordFixerFactory())
            .add(Transformer.parameterAnnotationFixerFactory())
            .add(Transformer.sourceFixerFactory(SourceFixerConfig.JAVA))
            .add(Transformer.signatureStripperFactory(SignatureStripperConfig.ALL))
            .logger(Logger::debug) // quiet
            .build()
            .run();

        // Restore file
        try {
            Files.move(tempOutput, outputJar, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (final AccessDeniedException ex) {
            // Sometimes because of file locking this will fail... Let's just try again and hope for the best
            // Thanks Windows!
            for (int tries = 0; tries < InstallerMain.MAX_TRIES; ++tries) {
                // Pause for a bit
                try {
                    Thread.sleep(5 * tries);
                    Files.move(tempOutput, outputJar, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (final AccessDeniedException ex2) {
                    if (tries == InstallerMain.MAX_TRIES - 1) {
                        throw ex;
                    }
                } catch (final InterruptedException exInterrupt) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            }
        }

        return outputJar;
    }
}
