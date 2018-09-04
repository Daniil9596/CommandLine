package com.cmdline;

import org.apache.commons.compress.archivers.*;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;

abstract class AbstractCommand {
    protected String command = "";
    protected String[] args = new String[0];

    protected void setCommand(String command) {
        this.command = command;
    }

    protected void setArgs(String[] args) {
        this.args = args;
    }

    //DO NOT FORGET ADD COMMAND CLASS TO THIS MAP<CommandString, CommandClass>
    protected static Map<String, Class<? extends AbstractCommand>> mapOfCommands = new HashMap<>();
    static {
        mapOfCommands.put("noSuchCommand", NoSuchCommand.class);
        mapOfCommands.put("exit", ExitCommand.class);
        mapOfCommands.put("listCommands", ListCommandsCommand.class);
        mapOfCommands.put("help", HelpCommand.class);
        mapOfCommands.put("currentPath", CurrentPathCommand.class);
        mapOfCommands.put("changePath", ChangePathCommand.class);
        mapOfCommands.put("listDir", ListDirCommand.class);
        mapOfCommands.put("makeDir", MakeDirCommand.class);
        mapOfCommands.put("remove", RemoveCommand.class);
        mapOfCommands.put("copy", CopyCommand.class);
        mapOfCommands.put("move", MoveCommand.class);
        mapOfCommands.put("print", PrintCommand.class);
        mapOfCommands.put("archive", ArchiveCommand.class);
    }

    abstract public String execute(Path currentAbsolutePath);
    abstract public String showUsage();

    public static AbstractCommand parseCommandWithArgs(String lineCommand) {
        String[] cmdWithArgs = lineCommand.trim().split("\\s+");
        if(cmdWithArgs.length > 0) {
            if(mapOfCommands.containsKey(cmdWithArgs[0])) {
                Class<? extends AbstractCommand> commandClass = mapOfCommands.get(cmdWithArgs[0]);
                AbstractCommand commandInstance = null;
                try {
                    commandInstance = commandClass.newInstance();
                    commandInstance.setArgs(Arrays.copyOfRange(cmdWithArgs, 1, cmdWithArgs.length));
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
                return commandInstance;
            }
        }
        return new NoSuchCommand();
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder(command);
        for(String arg : args) {
            res.append(" " + arg);
        }
        return res.toString();
    }
}

//Service commands of command line ---------------------------------------
class NoSuchCommand extends AbstractCommand {
    @Override
    public String execute(Path currentAbsolutePath) {
        return "No such command!\n" +
               "Show available commands: listCommands\n";
    }

    @Override
    public String showUsage() {
        return "";
    }
}

class ExitCommand extends AbstractCommand {
    @Override
    public String execute(Path currentAbsolutePath) {
        return "exit";
    }

    @Override
    public String showUsage() {
        return "Press \"exit\" to exit from command line";
    }
}

class ListCommandsCommand extends AbstractCommand {
    @Override
    public String execute(Path currentAbsolutePath) {
        System.out.println("Command line contains next commands: ");
        int i = 1;
        for(String command : AbstractCommand.mapOfCommands.keySet()) {
            if(!command.equals("noSuchCommand")) {
                System.out.println("[" + i++ + "] " + command);
            }
        }
        return "";
    }

    @Override
    public String showUsage() {
        return "Press \"listCommands\" to show all available commands";
    }
}

class HelpCommand extends AbstractCommand {
    @Override
    public String execute(Path currentAbsolutePath) {
        if(args.length > 0) {
            if(AbstractCommand.mapOfCommands.containsKey(args[0])) {
                Class<? extends AbstractCommand> commandClass = AbstractCommand.mapOfCommands.get(args[0]);
                try {
                    System.out.println(commandClass.newInstance().showUsage());
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            } else {
                return "Bad argument! No such command \"" + args[0] + "\"!";
            }
        } else {
            return showUsage();
        }
        return "";
    }

    @Override
    public String showUsage() {
        return "Press \"help\" to show usage of command\n" +
               "Usage: help <command>";
    }
}
//End of service commands of command line -----------------------------

class CurrentPathCommand extends AbstractCommand {
    @Override
    public String execute(Path currentAbsolutePath) {
        return currentAbsolutePath.toString();
    }

    @Override
    public String showUsage() {
        return "Press \"currentPath\" to show current absolute path in file system";
    }
}

class ChangePathCommand extends AbstractCommand {
    @Override
    public String execute(Path currentAbsolutePath) {
        if(args.length > 0) {
            try {
                Path path = Paths.get(args[0]);
                if(!path.isAbsolute()) {
                    path = currentAbsolutePath.resolve(path).normalize();
                }
                if(Files.isDirectory(path)) {
                    return path.toString();
                }
            } catch (InvalidPathException e) {
                System.out.println(e.getReason());
            }
        }
        return currentAbsolutePath.toString();
    }

    @Override
    public String showUsage() {
        return "Press \"changePath\" to change current absolute path of command line";
    }
}

class ListDirCommand extends AbstractCommand {
    @Override
    public String execute(Path currentAbsolutePath) {
        try {
            DirectoryStream<Path> stream = Files.newDirectoryStream(currentAbsolutePath);
            for(Path element : stream) {
                System.out.printf("%-32s size: %d\n", element.getFileName(), Files.size(element));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public String showUsage() {
        return "Press \"listDir\" to show current directory's content";
    }
}

class MakeDirCommand extends AbstractCommand {
    @Override
    public String execute(Path currentAbsolutePath) {
        if(args.length > 0) {
            Path dirPath = currentAbsolutePath.resolve(args[0]);
            try {
                Files.createDirectory(dirPath);
                return "";
            } catch (IOException e) {
                return "Error with creating new directory: " + args[0] + "!";
            }
        }
        return showUsage();
    }

    @Override
    public String showUsage() {
        return "Press \"makeDir <newDirectoryName>\" to create new directory";
    }
}

class RemoveCommand extends AbstractCommand {
    @Override
    public String execute(Path currentAbsolutePath) {
        if(args.length > 0) {
            Path removePath = currentAbsolutePath.resolve(args[0]);
            try {
                Files.deleteIfExists(removePath);
                return "";
            } catch (IOException e) {
                return "Error with removing: " + args[0] + "!";
            }
        }
        return showUsage();
    }

    @Override
    public String showUsage() {
        return "Press \"remove <file or directory>\" to remove file or directory";
    }
}

class CopyCommand extends AbstractCommand {
    @Override
    public String execute(Path currentAbsolutePath) {
        if(args.length > 1) {
            Path srcPath = currentAbsolutePath.resolve(args[0]);
            Path dstPath = currentAbsolutePath.resolve(args[1]);
            try {
                Files.copy(srcPath, dstPath);
                return "";
            } catch (IOException e) {
                return "Error with copying: " + args[0] + " -> " + args[1];
            }
        }
        return showUsage();
    }

    @Override
    public String showUsage() {
        return "Press \"copy <source file or directory> <destination file or directory>\" to copy file or directory";
    }
}

class MoveCommand extends AbstractCommand {
    @Override
    public String execute(Path currentAbsolutePath) {
        if(args.length > 1) {
            Path oldPath = currentAbsolutePath.resolve(args[0]);
            Path newPath = currentAbsolutePath.resolve(args[1]);
            try {
                Files.move(oldPath, newPath);
                return "";
            } catch (IOException e) {
                return "Error with moving: " + args[0] + " -> " + args[1];
            }
        }
        return showUsage();
    }

    @Override
    public String showUsage() {
        return "Press \"move <old path> <new path>\" to move or rename file or directory";
    }
}

class PrintCommand extends AbstractCommand {
    @Override
    public String execute(Path currentAbsolutePath) {
        if(args.length > 0) {
            Path filePath = currentAbsolutePath.resolve(args[0]);
            if(!Files.isDirectory(filePath)) {
                try {
                    List<String> fileLines = Files.readAllLines(filePath);
                    for(String line : fileLines) {
                        System.out.println(line);
                    }
                    return "";
                } catch (IOException e) {
                    return "Error with printing file: " + args[0];
                }
            }
        }
        return showUsage();
    }

    @Override
    public String showUsage() {
        return "Press \"print <file>\" to print file's content";
    }
}

class ArchiveCommand extends AbstractCommand {
    @Override
    public String execute(Path currentAbsolutePath) {
        if(args.length > 1) {
            if (args[0].equals("put")) {
                //put <file> or directory to archive <file>.zip
                Path filePath = currentAbsolutePath.resolve(args[1]);
                if(Files.exists(filePath)) {
                    try {
                        return ZipHelper.zip(filePath).toString();
                    } catch(IOException e) {
                        return e.getMessage();
                    }
                } else {
                    return "No such file: " + filePath.getFileName();
                }
            } else if (args[0].equals("get")) {
                //get <file> or directory from archive <file>.zip
                Path zipPath = currentAbsolutePath.resolve(args[1]);
                if(Files.exists(zipPath)) {
                    try {
                        return ZipHelper.unzip(zipPath).toString();
                    } catch(IOException e) {
                        return e.getMessage();
                    }
                } else {
                    return "No such zip file: " + zipPath.getFileName();
                }
            }
        }
        return showUsage();
    }

    @Override
    public String showUsage() {
        return "Press \"archive [put | get] <file>\" to (put in / get from) archive file or directory";
    }

    static class ZipHelper {
        public static Path zip(Path filePath) throws IOException {
            Path zipPath = getZipPathFromFilePath(filePath);
            FileOutputStream fos = new FileOutputStream(zipPath.toString());
            ZipArchiveOutputStream zipOut = new ZipArchiveOutputStream(fos);

            zipFile(filePath, filePath.getFileName().toString(), zipOut);

            zipOut.close();
            fos.close();
            return zipPath;
        }

        public static Path unzip(Path archivePath) throws IOException {
            Path extractedFilePath = archivePath.getParent();
            byte[] byteBuffer = new byte[1024];
            ZipArchiveInputStream zipInput = new ZipArchiveInputStream(new FileInputStream(archivePath.toString()));
            ZipArchiveEntry zipEntry = zipInput.getNextZipEntry();
            if(zipEntry != null) {
                extractedFilePath = extractedFilePath.resolve(Paths.get(zipEntry.getName()).subpath(0, 1));
            }
            while (zipEntry != null) {
                String fileName = zipEntry.getName();
                Path extractedPath = archivePath.resolveSibling(fileName);

                if(Files.isDirectory(extractedPath)) {
                    Files.createDirectories(extractedPath);
                } else {
                    Files.createDirectories(extractedPath.getParent());
                }

                FileOutputStream fos = new FileOutputStream(extractedPath.toString());
                int length;
                while ((length = zipInput.read(byteBuffer)) > 0) {
                    fos.write(byteBuffer, 0, length);
                }
                fos.close();
                zipEntry = zipInput.getNextZipEntry();
            }
            zipInput.close();
            return extractedFilePath;
        }

        private static Path getZipPathFromFilePath(Path filePath) {
            String fileName = filePath.getFileName().toString();
            int pointPos = fileName.indexOf(".");
            if(pointPos != -1) {
                fileName = fileName.substring(0, pointPos);
            }
            return filePath.resolveSibling(fileName + ".zip");
        }


        private static void zipFile(Path fileToZip, String fileName, ZipArchiveOutputStream zipOut) throws IOException {
            if (Files.isHidden(fileToZip)) {
                return;
            }

            if(Files.isDirectory(fileToZip)) {
                try(DirectoryStream<Path> dirStream = Files.newDirectoryStream(fileToZip)) {
                    for(Path childPath : dirStream) {
                        zipFile(childPath, fileName + "/" + childPath.getFileName().toString(), zipOut);
                    }
                }
                return;
            }

            InputStream fis = Files.newInputStream(fileToZip);
            ZipArchiveEntry zipEntry = new ZipArchiveEntry(fileName);
            zipOut.putArchiveEntry(zipEntry);
            byte[] byteBuffer = new byte[1024];
            int length;
            while ((length = fis.read(byteBuffer)) >= 0) {
                zipOut.write(byteBuffer, 0, length);
            }
            zipOut.closeArchiveEntry();
            fis.close();
        }
    }
}
//--------------------------------------------------------------------

public class CommandLine {
    //private List<AbstractCommand> commandHistory = new LinkedList<>();
    private static Path currentAbsolutePath = Paths.get(".").toAbsolutePath().normalize();

    private static void setCurrentAbsolutePath(String strPath) {
        currentAbsolutePath = Paths.get(strPath).toAbsolutePath();
    }

    public static void main(String[] args) {
        System.out.println("Welcome to simple command line!\n" +
                           "Main Usage: <command> [arg0[, arg1[, args...]]]\n\n" +
                           "Show available commands: listCommands\n" +
                           "Show usage of command  : help <command>\n" +
                           "Close command line     : exit\n");
        Scanner scanner = new Scanner(System.in);
        String result;
        do {
            System.out.println(currentAbsolutePath + "$ ");
            AbstractCommand command = AbstractCommand.parseCommandWithArgs(scanner.nextLine());
            result = command.execute(currentAbsolutePath);
            if(command instanceof ChangePathCommand) {
                setCurrentAbsolutePath(result);
            } else {
                System.out.println(result);
            }
        } while (!result.equals("exit"));
    }
}
