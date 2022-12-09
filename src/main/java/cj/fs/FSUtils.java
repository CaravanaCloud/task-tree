package cj.fs;

import cj.spi.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class FSUtils {
    private static final Logger log = LoggerFactory.getLogger(FSUtils.class);

    public static Path getLocalConfigDir() {
        return applicationDir().resolve("config");
    }

    public static String cwd() {
        return System.getProperty("user.dir");
    }

    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    public static void writeFile(Path path, String content) {
        try {
            Files.writeString(path, content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isEmptyDir(Path path) {
        if (Files.isDirectory(path)) {
            try (Stream<Path> entries = Files.list(path)) {
                return entries.findFirst().isEmpty();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    public static Path getLookupPath() {
        return currentDir();
    }

    public static String basename(Path path) {
        var name = path.getFileName();
        var names = name.toString().split("\\.");
        @SuppressWarnings("UnnecessaryLocalVariable")
        var base = names[0];
        return base;
    }

    public static void writeEnv(String varName, Path varValue) {
        var envFile = currentDir().resolve(".env");
        if (! envFile.toFile().exists()) {
            try {
                Files.createFile(envFile);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        var content = "\n%s=%s".formatted(varName, varValue);
        try {
            Files.write(envFile,
                    content.getBytes(),
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void copyDirWithBackup(Path src, Path dst) {
        var visitor = new CopyWithBackup(src, dst);
        try{
            Files.walkFileTree(src, visitor);
        }catch (IOException e){
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static void copyFileWithBackup(Path newkubeconfig, Path newconfig) {
        if (newconfig.toFile().exists()){
            var newFileName = newconfig.getFileName() + "." + System.currentTimeMillis() + ".bak";
            var backup = newconfig.getParent().resolve(newFileName);
            try {
                Files.copy(newconfig, backup, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }
        try {
            Files.copy(newkubeconfig, newconfig, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static String format(LocalDateTime createTime) {
        return formatter.format(createTime);
    }


    static class CopyWithBackup extends SimpleFileVisitor<Path>{
        private final Path src;
        private final Path dst;

        public CopyWithBackup(Path src, Path dst) {
            this.src = src;
            this.dst = dst;
        }

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs){
            log.trace("Copying file: {}", path);
            var relative = src.relativize(path);
            var dstPath = dst.resolve(relative);
            backup(dstPath);
            copy(path, dstPath);
            return FileVisitResult.CONTINUE;
        }

        private void copy(Path path, Path dstPath) {
            try {
                Files.copy(path, dstPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }
            log.debug("copy {} => {}", path, dstPath);
        }

        private void backup(Path dstPath) {
            if (dstPath.toFile().exists()) {
                var timestamp = ""+System.currentTimeMillis();
                var backupstamp = "."+timestamp+".bak";
                var backupPath = dstPath.resolveSibling(dstPath.getFileName() +  backupstamp);
                log.trace("Backing up file: {}", dstPath);
                try {
                    Files.move(dstPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }
        }


    }


    static class FindFiles extends SimpleFileVisitor<Path> {
        List<Path> results = new ArrayList<>();
        String extension;

        public FindFiles(String extension) {
            this.extension = extension;
        }

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs){
            log.trace("visiting file: {}", path);
            if (filterMatch(path)){
                results.add(path);
            }
            return FileVisitResult.CONTINUE;
        }

        private boolean filterMatch(Path path) {
            var match=true;
            if (extension != null){
                match = path.toFile().getName().endsWith(extension);
            }
            log.trace("Path {} match? {}", path, match);
            return match;
        }

        public List<Path> getResults() {
            return results;
        }
    }

    public static List<Path> findByExtension(Path dir, String extension) {
        if(dir.toFile().exists()){
            log.debug("Looking for files on {}", dir);
            var visitor = new FindFiles(extension);
            try{
                Files.walkFileTree(dir, visitor);
            }catch (IOException e){
                log.error(e.getMessage(), e);
            }
            @SuppressWarnings("VariableTypeCanBeExplicit")
            var results = visitor.getResults();
            return  results;
        }else{
            log.debug("Directory does not exist, visitor not started. {}", dir);
            return List.of();
        }
    }

    public static List<Path> filterLocalVideos(){
        var extension = "mp4";
        var currentDirMatch = findByExtension(currentDir(), extension);
        return Stream.of(currentDirMatch)
                .flatMap(Collection::stream)
                .toList();
    }

    private static Path currentDir() {
        Path userPath = Paths.get(System.getProperty("user.dir"));
        return userPath;
    }

    public static Path applicationDir() {
        Path homePath = getHomePath();
        var configPath = homePath.resolve(".config");
        var appPath = resolveDir(configPath, "cloud-janitor");
        return appPath;
    }
    
    public static Path tasksDir(){
        return resolveDir(applicationDir(), "tasks");
    }

    public static Path taskDir(Task task, String context){
        return resolveDir(taskDir(task), context);
    }

    public static Path taskDir(Task task) {
        return taskDir(task.getPathName());
    }

    public static Path taskDir(String path) {
        return resolveDir(tasksDir(), path);
    }

    public static Path dataDir(){
        return resolveDir(applicationDir(), "data");
    }

    public static Path dataDir(String... context) {
        var dataDir = dataDir();
        for (String s : context) {
            dataDir = resolveDir(dataDir, s);
        }
        return dataDir;
    }

    public static Path resolveDir(Path parent, String target) {
        if (parent == null || target == null) {
            throw new IllegalArgumentException("Parent and target must not be null");
        }
        var dataPath = parent.resolve(target);
        var dataDir = dataPath.toFile();
        if (! dataDir.exists()){
            dataDir.mkdirs();
        }
        return dataPath;
    }

    public static Path getVideosDir() {
        Path homePath = getHomePath();
        var path = homePath.resolve("Videos");
        return path;
    }


    public static Path getHomePath() {
        var home = System.getProperty("user.home");
        var homePath = Path.of(home);
        return homePath;
    }

}
