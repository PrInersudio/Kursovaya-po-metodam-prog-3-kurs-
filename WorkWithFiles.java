import java.util.zip.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
public class WorkWithFiles {

    private static void createDirectiory(String filename) throws Exception {
        String[] path = filename.split("/");
        if (path.length == 0) return;
        String folder = "";
        for (int i = 0; i < path.length - 1; i++)
            folder += path[i] + "/";
        Files.createDirectories(Paths.get(folder));
    }
    
    public static void unzip(String archive_name, String folder) throws Exception {
        ZipInputStream archive = new ZipInputStream(new FileInputStream(archive_name));
        ZipEntry entry = null;
        while (true) {
            try {
                if ((entry = archive.getNextEntry()) == null) break;
                String path = folder + "/" + entry.getName();
                createDirectiory(path);
                FileOutputStream unarchived_file = new FileOutputStream(path);
                unarchived_file.write(archive.readAllBytes());
                unarchived_file.flush();
                unarchived_file.close();
                archive.closeEntry();
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        archive.close();
    }

    public static String[] getFilesInFolderNames (String folder_name) {
        File folder = new File(folder_name);
        File[] files = folder.listFiles();
        String[] filenames = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            filenames[i] = folder_name + "/" + files[i].getName();
        }
        return filenames;
    }

    private static String cutOriginalFolder(String filename, String folder_name) {
        String[] path = filename.split("/");
        filename = path[folder_name.split("/").length-1];
        for (int i = folder_name.split("/").length; i < path.length; i++)
            filename += "/" + path[i];
        return filename;
    }

    private static void zipFile(String file_name, String folder_name, ZipOutputStream archive) throws Exception {
        ZipEntry entry = new ZipEntry(cutOriginalFolder(file_name, folder_name));
        archive.putNextEntry(entry);
        FileInputStream unarchived_file = new FileInputStream(file_name);
        byte[] buffer = new byte[unarchived_file.available()];
        unarchived_file.read(buffer);
        archive.write(buffer);
        unarchived_file.close();
        archive.closeEntry();
    }

    private static void zipFolder(String folder_name, ZipOutputStream archive, String original_folder) throws Exception {
        String[] filenames =  getFilesInFolderNames(folder_name);
        for (int i = 0; i < filenames.length; i++)
            try {
                if (Files.isDirectory(Paths.get(filenames[i])))
                    zipFolder(filenames[i], archive, original_folder);
                else
                    zipFile(filenames[i], original_folder, archive);
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
            }
    }
    
    public static void zip(String archive_name, String[] filenames) throws Exception {
        ZipOutputStream archive = new ZipOutputStream(new FileOutputStream(archive_name));
        for (int i = 0; i < filenames.length; i++) {
            try {
                if (Files.isDirectory(Paths.get(filenames[i])))
                    zipFolder(filenames[i], archive, filenames[i]);
                else
                    zipFile(filenames[i], filenames[i], archive);
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        archive.close();
    }

    public static void RecursiveDelete(File folder) {
        if  (folder.isDirectory())
            for (File subfolder : folder.listFiles())
                RecursiveDelete(subfolder);
        folder.delete();
    }
}
