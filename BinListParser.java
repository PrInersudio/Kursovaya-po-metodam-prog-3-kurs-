import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
public class BinListParser {

    private static String getResponse(int IIN) throws Exception {
        try {
            URL connection = new URL("https://lookup.binlist.net/" + IIN);
            InputStream connection_input_stream = connection.openStream();
            byte[] input = new byte[connection_input_stream.available()];
            connection_input_stream.read(input);
            connection_input_stream.close();
            return new String(input, StandardCharsets.UTF_8);
        }
        catch (MalformedURLException e) {
            System.out.println("Не удалось подключиться к https://lookup.binlist.net/" + IIN);
        }
        catch (IOException e) {
            System.out.println("Не удалось чтение с https://lookup.binlist.net/" + IIN);
        }
        return null;
    }

    private static String parseField(String json, String field_name, boolean is_big_field) {
        int field_position = json.indexOf(field_name);
        if (field_position == -1)
            return null;
        field_position += field_name.length() + 2;
        char end_symbol = ',';
        if (is_big_field) end_symbol = '}';
        int end_of_field_position = json.indexOf(end_symbol, field_position);
        if (end_of_field_position == -1) end_of_field_position = json.length();
        return json.substring(field_position, end_of_field_position);
    }

    private static String getStringFieldWithoutQuotes(String json, String field_name) throws Exception {
        String field = parseField(json, field_name, false);
        if (field == null) return "Not found";
        if (field.charAt(0) != '"')
            throw new Exception(field + " не является строковым полем");
        return field.substring(1, field.length()-1);
    }

    public static DatabaseEntry getDatabaseEntry(int IIN) throws Exception {
        String response = getResponse(IIN);
        if (response == null) return null;
        DatabaseEntry data = new DatabaseEntry();
        data.IIN = IIN;
        String field_string = parseField(response, "length", false);
        data.length = field_string == null ? -1 : Integer.parseInt(field_string);
        data.pay_system = getStringFieldWithoutQuotes(response, "scheme");
        data.pay_system = String.valueOf(Character.toUpperCase(data.pay_system.charAt(0))) + data.pay_system.substring(1);
        data.type = getStringFieldWithoutQuotes(response, "type");
        data.brand = getStringFieldWithoutQuotes(response, "brand");
        data.country = getStringFieldWithoutQuotes(parseField(response, "country", true), "name");
        data.bank = new DatabaseEntry.BankInfo();
        String bank_json = parseField(response, "bank", true);
        data.bank.name = getStringFieldWithoutQuotes(bank_json, "name");
        data.bank.url = getStringFieldWithoutQuotes(bank_json, "url");
        return data;
    }
}