import java.io.*;
import java.nio.charset.StandardCharsets;
public class Database {

    private static String readTo (RandomAccessFile filepointer, char border) throws Exception {
        String result = "";
        char current_symbol;
        while ((current_symbol = (char)filepointer.read()) != border)
            result += String.valueOf(current_symbol);
        return result;
    }

    private static String getNextFieldName (RandomAccessFile filepointer) throws Exception {
        while ((char)filepointer.read() != '<');
        return readTo(filepointer, '>');
    }

    private static String findField (RandomAccessFile filepointer, String field_name) throws Exception {
        String field;
        while (true) {
            field = getNextFieldName(filepointer);
            if ((field.equals("/sst")) || (field.equals("/worksheet"))) return field;
            if ((field.length() >= field_name.length()) && (field.substring(0, field_name.length()).equals(field_name))) break;
        }
        return field;
    }
    
    private static int getEntriesQuantity(RandomAccessFile worksheet) throws Exception {
        String dimension_ref = findField(worksheet, "dimension ref=");
        if (dimension_ref.equals("/worksheet"))
            throw new Exception("Поле dimension ref не найдёно");
        int start_of_size = dimension_ref.indexOf(":G") + 2;
        int end_of_size = dimension_ref.indexOf('"', start_of_size);
        return Integer.parseInt(dimension_ref.substring(start_of_size, end_of_size));
    }

    private static String assembleSharedString(RandomAccessFile sharedStrings) throws Exception {
        String shared_string = "";
        String next_field_name = getNextFieldName(sharedStrings);
        while (next_field_name.equals("/si") == false) {
            if (next_field_name.charAt(0) == 't')
                shared_string += readTo(sharedStrings, '<');
            if (next_field_name.equals("/sst"))
                throw new Exception("Неожиданно встречен конец sharedStrings при попытке получить строку");
            next_field_name = getNextFieldName(sharedStrings);
        }
        return shared_string;
    }

    private static String getNextSharedString(RandomAccessFile sharedStrings) throws Exception {
        if (findField(sharedStrings, "si").equals("/sst"))
                return null;
        String shared_string = assembleSharedString(sharedStrings);
        return shared_string;
    }

    private static String getSharedString (RandomAccessFile sharedStrings, int string_number) throws Exception {
        for (int i = 0; i <= string_number; i++)
            if (findField(sharedStrings, "si").equals("/sst"))
                return null;
        String shared_string = assembleSharedString(sharedStrings);
        sharedStrings.seek(0);
        return shared_string;
    }
    
    private static String parseColumn(RandomAccessFile worksheet, String column_name, RandomAccessFile sharedStrings) throws Exception {
        String column_field = findField(worksheet, "c r=" + '"' + column_name);
        if(column_field.equals("/worksheet"))
            return null;
        int i = 0;
        while ((i < column_field.length()) && (column_field.charAt(i) != 't')) i++;
        boolean is_string = false;
        if ((i < column_field.length()) && (column_field.charAt(i+3)) == 's') is_string = true;
        if(findField(worksheet, "v").equals("/worksheet"))
            throw new Exception("Значение колонки " + column_name + " не найдено");
        String column_value = readTo(worksheet, '<');
        if (is_string) {
            String shared_string = getSharedString(sharedStrings, Integer.parseInt(column_value));
            if (shared_string == null)
                throw new Exception("Строка колонки " + column_name + " не найдена");
            return shared_string;
        }
        return column_value;
    }
    
    private static int getIIN(int index, RandomAccessFile worksheet1, RandomAccessFile sharedStrings) throws Exception {
        return Integer.parseInt(parseColumn(worksheet1, "A" + index, sharedStrings));
    }
    
    private static int findEntry(int IIN, RandomAccessFile worksheet1, RandomAccessFile sharedStrings) throws Exception {
        int left_index = 1;
        int right_index = getEntriesQuantity(worksheet1);
        while (left_index <= right_index) {
            int middle_index = (left_index+right_index)/2;
            int IIN_in_middle_index = getIIN(middle_index, worksheet1, sharedStrings);
            worksheet1.seek(0);
            if (IIN_in_middle_index == IIN) return middle_index;
            else if (IIN_in_middle_index < IIN) left_index = middle_index + 1;
            else right_index = middle_index-1;
        }
        return -1;
    }

    private static int getLenght(int index, RandomAccessFile worksheet1, RandomAccessFile sharedStrings) throws Exception {
        return Integer.parseInt(parseColumn(worksheet1, "B" + index, sharedStrings));
    }

    private static String getPaySystem(int index, RandomAccessFile worksheet1, RandomAccessFile worksheet2, RandomAccessFile sharedStrings) throws Exception {
        return parseColumn(worksheet2, "A" + parseColumn(worksheet1, "C" + index, sharedStrings), sharedStrings);
    }

    private static String getType(int index, RandomAccessFile worksheet1, RandomAccessFile sharedStrings) throws Exception {
        return parseColumn(worksheet1, "D" + index, sharedStrings);
    }

    private static String getBrand(int index, RandomAccessFile worksheet1, RandomAccessFile worksheet3, RandomAccessFile sharedStrings) throws Exception {
        return parseColumn(worksheet3, "A" + parseColumn(worksheet1, "E" + index, sharedStrings), sharedStrings);
    }

    private static String getCountry(int index, RandomAccessFile worksheet1, RandomAccessFile worksheet4, RandomAccessFile sharedStrings) throws Exception {
        return parseColumn(worksheet4, "A" + parseColumn(worksheet1, "F" + index, sharedStrings), sharedStrings);
    }

    private static DatabaseEntry.BankInfo getBank(int index, RandomAccessFile worksheet1, RandomAccessFile worksheet5, RandomAccessFile sharedStrings) throws Exception {
        String bank_number = parseColumn(worksheet1, "G" + index, sharedStrings);
        DatabaseEntry.BankInfo bank = new DatabaseEntry.BankInfo();
        bank.name = parseColumn(worksheet5, "A" + bank_number, sharedStrings);
        bank.url = parseColumn(worksheet5, "B" + bank_number, sharedStrings);
        return bank;
    }

    private static void resetOffsets(RandomAccessFile[] worksheets, RandomAccessFile sharedStrings) throws Exception {
        for (int i = 0; i < worksheets.length; i++)
            worksheets[i].seek(0);
        sharedStrings.seek(0);
    }

    public static DatabaseEntry getData(int IIN, RandomAccessFile[] worksheets, RandomAccessFile sharedStrings) throws Exception {
        int index = findEntry(IIN, worksheets[0], sharedStrings);
        if (index == -1)
            return null;
        DatabaseEntry data = new DatabaseEntry();
        data.IIN = getIIN(index, worksheets[0], sharedStrings);
        data.length = getLenght(index, worksheets[0], sharedStrings);
        data.pay_system = getPaySystem(index, worksheets[0], worksheets[1], sharedStrings);
        data.type = getType(index, worksheets[0], sharedStrings);
        data.brand = getBrand(index, worksheets[0], worksheets[2], sharedStrings);
        data.country = getCountry(index, worksheets[0], worksheets[3], sharedStrings);
        data.bank = getBank(index, worksheets[0], worksheets[4], sharedStrings);
        resetOffsets(worksheets, sharedStrings);
        return data;
    }

    private static class ExcelLine {
    
        public int IIN;
        public int length;
        public int pay_system_number;
        public int type_shared_string_number;
        public int brand_number;
        public int country_number;
        public int bank_number;
    
        public ExcelLine(DatabaseEntry data, RandomAccessFile[] worksheets, RandomAccessFile sharedStrings) throws Exception {
            IIN = data.IIN;
            length = data.length;
            pay_system_number = getNameNumber(worksheets[1], sharedStrings, new String[] {data.pay_system});
            type_shared_string_number = getSharedStringNumber(data.type, sharedStrings);
            brand_number = getNameNumber(worksheets[2], sharedStrings, new String[] {data.brand});
            country_number = getNameNumber(worksheets[3], sharedStrings, new String[] {data.country});
            bank_number = getNameNumber(worksheets[4], sharedStrings, new String[] {data.bank.name, data.bank.url});
        }

        public byte[] makeLineForSheet1(int index) {
            String field =
                "<row r=" + '"' + index + '"' + " spans=" + '"' +"1:7" + '"' + " x14ac:dyDescent=" + '"' + "0.25" + '"' + ">" +
                    "<c r=" + '"' + "A" + index + '"' + " s=" + '"' + "1" + '"' + ">" +
                        "<v>" + IIN + "</v>" +
                    "</c>" +
                    "<c r=" + '"' + "B" + index + '"' + ">" +
                        "<v>" + length + "</v>" +
                    "</c>" +
                    "<c r=" + '"' + "C" + index + '"' + ">" +
                        "<v>" + pay_system_number + "</v>" +
                    "</c>" +
                    "<c r=" + '"' + "D" + index + '"' + " t=" + '"' + "s" + '"' + ">" +
                        "<v>" + type_shared_string_number + "</v>" +
                    "</c>" +
                    "<c r=" + '"' + "E" + index + '"' + ">" +
                        "<v>" + brand_number + "</v>" +
                    "</c>" +
                    "<c r=" + '"' + "F" + index + '"' + ">" +
                        "<v>" + country_number + "</v>" +
                    "</c>" +
                    "<c r=" + '"' + "G" + index + '"' + ">" +
                        "<v>" + bank_number + "</v>" +
                    "</c>" +
                "</row>";
            return field.getBytes(StandardCharsets.UTF_8);
        }
    }

    private static String parseField(String field, String name_of_parameter) throws Exception {
        int start = field.indexOf(name_of_parameter);
        if (start == -1)
            throw new Exception("Нет параметра " + name_of_parameter + " в поле " + field);
        start += name_of_parameter.length() + 2;
        int end = field.indexOf('"', start);
        return field.substring(start, end);
    }

    private static byte[] concatinateBytes(byte[] array1, byte[] array2) {
        if (array1 == null) return array2;
        if (array2 == null) return array1;
        byte[] new_array = new byte[array1.length + array2.length];
        for (int i = 0; i < array1.length; i++)
            new_array[i] = array1[i];
        for (int i = 0; i < array2.length; i++)
            new_array[i+array1.length] = array2[i];
        return new_array;
    }

    private static void addSharedString(String shared_string, RandomAccessFile sharedStrings, int shared_strings_quantity) throws Exception {
        sharedStrings.seek(sharedStrings.length() - 6);
        String new_shared_string_field = "<si><t>" + shared_string + "</t></si>";
        sharedStrings.write(new_shared_string_field.getBytes(StandardCharsets.UTF_8));
        sharedStrings.write("</sst>".getBytes(StandardCharsets.UTF_8));
        sharedStrings.seek(0);
        String sst_string = findField(sharedStrings, "sst");
        byte[] buffer1 = new byte[(int)sharedStrings.getFilePointer() - sst_string.length() - 2];
        sharedStrings.seek(0);
        sharedStrings.read(buffer1);
        sharedStrings.seek(sharedStrings.getFilePointer() + sst_string.length() + 2);
        byte[] buffer2 = new byte[(int)(sharedStrings.length() - sharedStrings.getFilePointer())];
        sharedStrings.read(buffer2);
        sharedStrings.seek(0);
        int count = Integer.parseInt(parseField(sst_string, "count")) + 1;
        sst_string = "<" + sst_string.substring(0, sst_string.indexOf("count") + 7) + count + sst_string.substring(sst_string.indexOf("uniqueCount") - 2, sst_string.indexOf("uniqueCount") + 13) + shared_strings_quantity + '"' + ">";
        sharedStrings.write(buffer1);
        sharedStrings.write(sst_string.getBytes(StandardCharsets.UTF_8));
        sharedStrings.write(buffer2);
    }

    private static int getSharedStringNumber(String shared_string, RandomAccessFile sharedStrings) throws Exception {
        String next_shared_string = getNextSharedString(sharedStrings);
        int index = 0;
        while ((next_shared_string != null) && (shared_string.equals(next_shared_string) == false)) {
            next_shared_string = getNextSharedString(sharedStrings);
            index++;
        }
        if (next_shared_string == null)
            addSharedString(shared_string, sharedStrings, index);
        else {
            sharedStrings.seek(0);
            String sst_string = findField(sharedStrings, "sst");
            byte[] buffer1 = new byte[(int)sharedStrings.getFilePointer() - sst_string.length() - 2];
            sharedStrings.seek(0);
            sharedStrings.read(buffer1);
            sharedStrings.seek(sharedStrings.getFilePointer() + sst_string.length() + 2);
            byte[] buffer2 = new byte[(int)(sharedStrings.length() - sharedStrings.getFilePointer())];
            sharedStrings.read(buffer2);
            sharedStrings.seek(0);
            int count = Integer.parseInt(parseField(sst_string, "count")) + 1;
            sst_string = "<" + sst_string.substring(0, sst_string.indexOf("count") + 7) + count + sst_string.substring(sst_string.indexOf("uniqueCount") - 2) + ">";
            sharedStrings.write(buffer1);
            sharedStrings.write(sst_string.getBytes(StandardCharsets.UTF_8));
            sharedStrings.write(buffer2);
        }
        sharedStrings.seek(0);
        return index;
    }

    private static byte[] getPart(RandomAccessFile file, long offset) throws Exception {
        long offset2 = file.getFilePointer();
        byte[] buffer = new byte[(int)(offset2 - offset)];
        file.seek(offset);
        file.read(buffer);
        return buffer;
    }

    private static String getNextColumnName(String column) {
        if (column.charAt(column.length() - 1) == 'Z') {
            String next_column = "A";
            for (int i = 0; i < column.length(); i++)
                next_column += "A";
            return next_column;
        }
        return column.substring(0, column.length() - 1) + String.valueOf((char)(column.charAt(column.length()-1) + 1));
    }

    private static byte[] getNewLine (int[] shared_string_numbers, byte[] line_example_bytes, int index) {
        String line_example = new String(line_example_bytes, StandardCharsets.UTF_8);
        int offset = line_example.indexOf('"') + 1;
        String new_line = line_example.substring(0, offset) + index + '"';
        offset = line_example.indexOf('"', offset)+1;
        int offset2 = line_example.indexOf(":", offset)+1;
        new_line += line_example.substring(offset, offset2) + shared_string_numbers.length + '"';
        offset = line_example.indexOf('"', offset2) + 1;
        offset2 = line_example.indexOf('>', offset) + 1;
        new_line += line_example.substring(offset, offset2);
        String column_name = "A";
        for (int i = 0; i < shared_string_numbers.length; i++) {
            offset = offset2;
            offset2 = line_example.indexOf(column_name, offset);
            new_line += line_example.substring(offset, offset2) + column_name + index + '"';
            offset = line_example.indexOf('"', offset2) + 1;
            offset2 = line_example.indexOf("<v", offset);
            offset2 = line_example.indexOf(">", offset2) + 1;
            new_line += line_example.substring(offset, offset2) + shared_string_numbers[i];
            offset2 = line_example.indexOf('<', offset2);
            column_name = getNextColumnName(column_name);
        }
        offset = line_example.indexOf("</v>", offset2);
        new_line += line_example.substring(offset);
        return new_line.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] getLineExample(int number_of_lines) {
        String line_example = "<row r =" + '"' + "1" + '"' + " spans=" + '"' + "1:" + number_of_lines + '"' + ">";
        String column = "A";
        for (int i = 0; i < number_of_lines; i++) {
            line_example += "c r=" + '"' + column + "1" + '"' + " t=" + '"' + "s" + '"' + ">" +
                                "<v>0</v>" +
                            "</c>";
            column = getNextColumnName(column);
        }
        line_example += "</row>";
        return line_example.getBytes(StandardCharsets.UTF_8);
    }

    private static class createNewHeader {
        byte[] new_header;
        int old_size;
        public createNewHeader(RandomAccessFile worksheet) throws Exception {
            String dimension_ref = findField(worksheet, "dimension ref");
            if (dimension_ref.equals("/worksheet"))
                throw new Exception("Поле dimension ref не найдёно");
            new_header = new byte[(int)worksheet.getFilePointer() - dimension_ref.length() - 2];
            worksheet.seek(0);
            worksheet.read(new_header);
            worksheet.seek(worksheet.getFilePointer() + dimension_ref.length() + 2);
            int start_of_size = dimension_ref.indexOf(":") + 1;
            while ((dimension_ref.charAt(start_of_size) < '0') || (dimension_ref.charAt(start_of_size) > '9')) start_of_size++;
            int end_of_size = dimension_ref.indexOf('"', start_of_size);
            old_size = Integer.parseInt(dimension_ref.substring(start_of_size, end_of_size));
            dimension_ref = "<" + dimension_ref.substring(0, start_of_size) + (old_size+1) + dimension_ref.substring(end_of_size, dimension_ref.length()) + ">";
            new_header = concatinateBytes(new_header, dimension_ref.getBytes(StandardCharsets.UTF_8));
            long offest = worksheet.getFilePointer();
            if (findField(worksheet, "sheetData").equals("/worksheet"))
                throw new Exception("Поле sheetData не найдёно");
            new_header = concatinateBytes(new_header, getPart(worksheet, offest));
        }
    }

    private static byte[] incRow(String row) {
        int offest = row.indexOf('"') + 1;
        int offest2 = row.indexOf('"', offest);
        int number = Integer.parseInt(row.substring(offest, offest2)) + 1;
        String new_row = "<row r=" + '"' + number + '"' + row.substring(offest2+1) + ">";
        return new_row.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] incC(String c) {
        int offest = c.indexOf('"') + 1;
        while ((c.charAt(offest) > '9') || (c.charAt(offest) < '0'))
            offest++;
        int offest2 = c.indexOf('"', offest);
        int number = Integer.parseInt(c.substring(offest, offest2)) + 1;
        String new_c = "<" + c.substring(0, offest) + number + c.substring(offest2) + ">";
        return new_c.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] incBiggerLines(RandomAccessFile worksheet) throws Exception {
        byte[] buffer = null;
        long offest = worksheet.getFilePointer();
        String current_field = getNextFieldName(worksheet);
        while(current_field.equals("/sheetData") == false) {
            if (current_field.equals("/worksheet"))
                throw new Exception("Не встречен /sheetData");
            worksheet.seek(worksheet.getFilePointer() - current_field.length() - 2);
            buffer = concatinateBytes(buffer, getPart(worksheet, offest));
            worksheet.seek(worksheet.getFilePointer() + current_field.length() + 2);
            offest = worksheet.getFilePointer();
            if ((current_field.length() >= 3) && (current_field.substring(0,3).equals("row")))
                buffer = concatinateBytes(buffer, incRow(current_field));
            else if (current_field.charAt(0) == 'c')
                buffer = concatinateBytes(buffer, incC(current_field));
            else {
                current_field = "<" + current_field +">";
                buffer = concatinateBytes(buffer, current_field.getBytes(StandardCharsets.UTF_8));
            }
            current_field = getNextFieldName(worksheet);
        }
        byte[] last_part = new byte[(int)(worksheet.length() - worksheet.getFilePointer())];
        worksheet.read(last_part);
        current_field = "<" + current_field + ">";
        buffer = concatinateBytes(buffer, current_field.getBytes(StandardCharsets.UTF_8));
        return concatinateBytes(buffer, last_part);
    }
    
    private static void addNames(RandomAccessFile worksheet, int[] shared_string_numbers) throws Exception {
        createNewHeader new_header = new createNewHeader(worksheet);
        long offset = worksheet.getFilePointer();
        byte[] line_example;
        if (new_header.old_size == 0)
            line_example = getLineExample(shared_string_numbers.length);
        else {
            if (findField(worksheet, "/row").equals("/worksheet"))
                throw new Exception("Не удалось найти поле /row");
            line_example = getPart(worksheet, offset);
        }
        
        String sheetData_end = findField(worksheet, "/sheetData");
        if (sheetData_end.equals("/worksheet"))
                throw new Exception("Поле /sheetData не найдёно");
        worksheet.seek(worksheet.getFilePointer() - sheetData_end.length() - 2);
        byte[] buffer = concatinateBytes(new_header.new_header, getPart(worksheet, offset));
        buffer = concatinateBytes(buffer, getNewLine(shared_string_numbers, line_example, new_header.old_size + 1));
        sheetData_end = "<" + sheetData_end + ">";
        buffer = concatinateBytes(buffer, sheetData_end.getBytes(StandardCharsets.UTF_8));
        worksheet.seek(worksheet.getFilePointer() + sheetData_end.length());
        byte buffer2[] = new byte[(int)(worksheet.length() - worksheet.getFilePointer())];
        worksheet.read(buffer2);
        buffer = concatinateBytes(buffer, buffer2);
        worksheet.seek(0);
        worksheet.write(buffer);
    }

    private static int getNameNumber(RandomAccessFile worksheet, RandomAccessFile sharedStrings, String[] names) throws Exception {
        int[] shared_string_numbers = new int[names.length];
        for (int i = 0; i < shared_string_numbers.length; i++) 
           shared_string_numbers[i] = getSharedStringNumber(names[i], sharedStrings);
        int index = 1;
        String current_line_value = parseColumn(worksheet, "A" + index, sharedStrings);
        while (current_line_value != null) {
            if (names[0].equals(current_line_value)) {
                worksheet.seek(0);
                return index;
            }
            index++;
            current_line_value = parseColumn(worksheet, "A" + index, sharedStrings);
        }
        worksheet.seek(0);
        addNames(worksheet, shared_string_numbers);
        worksheet.seek(0);
        return index;
    }

    public static void addData(DatabaseEntry data, RandomAccessFile[] worksheets, RandomAccessFile sharedStrings) throws Exception {
        ExcelLine line_to_add = new ExcelLine(data, worksheets, sharedStrings);
        createNewHeader new_header = new createNewHeader(worksheets[0]);
        long offest = worksheets[0].getFilePointer();
        byte[] buffer;
        long offset2 = offest;
        if (new_header.old_size == 0)
            buffer = concatinateBytes(new_header.new_header, line_to_add.makeLineForSheet1(1));
        else {
            int index = 1;
            while (index <= new_header.old_size) {
                if (Integer.parseInt(parseColumn(worksheets[0], "A" + index, sharedStrings)) > line_to_add.IIN) break;
                if (findField(worksheets[0], "/row").equals("/worksheets"))
                    throw new Exception("Поле /row в строке " + index + " не найдено");
                offset2 = worksheets[0].getFilePointer();
                index++;
            }
            worksheets[0].seek(offset2);
            buffer = concatinateBytes(new_header.new_header, getPart(worksheets[0], offest));
            buffer = concatinateBytes(buffer, line_to_add.makeLineForSheet1(index));
        }
        buffer = concatinateBytes(buffer, incBiggerLines(worksheets[0]));
        worksheets[0].seek(0);
        worksheets[0].write(buffer);
        resetOffsets(worksheets, sharedStrings);
    }

    public static String getLastDBLines(RandomAccessFile worksheet1, RandomAccessFile sharedStrings) throws Exception {
        String dimension_ref = findField(worksheet1, "dimension ref");
        int offest = dimension_ref.indexOf(':') + 1;
        while ((dimension_ref.charAt(offest) < '0') || (dimension_ref.charAt(offest) > '9')) offest++;
        int offest2 = dimension_ref.indexOf('"', offest);
        int lines_quantity = Integer.parseInt(dimension_ref.substring(offest, offest2));
        int index = 0;
        String last_DB_lines = "";
        while (index < 10) {
            last_DB_lines += parseColumn(worksheet1, "A" + lines_quantity, sharedStrings) + "<br>";
            worksheet1.seek(0);
            lines_quantity--;
            index++;
            if (lines_quantity == 0) break;
        }
        return last_DB_lines;
    }
}