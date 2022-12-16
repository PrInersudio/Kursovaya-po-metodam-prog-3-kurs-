import java.io.*;
public class getLastDBLines {
    public static void main(String[] args) throws Exception {
        WorkWithFiles.unzip("database.xlsx", "DatabaseUnarchived");
        RandomAccessFile worksheet1 = new RandomAccessFile("DatabaseUnarchived/xl/worksheets/sheet1.xml", "rw");
        RandomAccessFile sharedStrings = new RandomAccessFile("DatabaseUnarchived/xl/sharedStrings.xml", "rw");
        System.out.println(Database.getLastDBLines(worksheet1, sharedStrings));
        worksheet1.close();
        sharedStrings.close();
        WorkWithFiles.RecursiveDelete(new File("DatabaseUnarchived"));
    }
}
