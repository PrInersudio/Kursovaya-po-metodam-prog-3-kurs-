import java.io.*;
public class Main {
    public static void main(String[] terminal_arguments) throws Exception {
        DatabaseEntry data = null;
        if ((terminal_arguments[0].length() != 6) && (terminal_arguments[0].length() != 8))
            System.out.println(terminal_arguments[0] + " не является IIN");
        int IIN = Integer.parseInt(terminal_arguments[0]);
        WorkWithFiles.unzip("database.xlsx", "DatabaseUnarchived");
        RandomAccessFile[] worksheets = new RandomAccessFile[5];
        for (int i = 1; i <= 5; i++)
            worksheets[i-1] = new RandomAccessFile("DatabaseUnarchived/xl/worksheets/sheet" + i + ".xml", "rw");
        RandomAccessFile sharedStrings = new RandomAccessFile("DatabaseUnarchived/xl/sharedStrings.xml", "rw");
        data = Database.getData(IIN, worksheets, sharedStrings);
        if (data == null) {
            data = BinListParser.getDatabaseEntry(IIN);
            if (data != null)
                Database.addData(data, worksheets, sharedStrings);
        }
        for (int i = 0; i < 5; i ++)
            worksheets[i].close();
        sharedStrings.close();
        WorkWithFiles.zip("database.xlsx", WorkWithFiles.getFilesInFolderNames("DatabaseUnarchived"));
        WorkWithFiles.RecursiveDelete(new File("DatabaseUnarchived"));
        if (data == null)
            System.out.println("Data not found");
        else
            System.out.println(data.getHTMLTable());
    }
}