public class DatabaseEntry {
    
    public static class BankInfo {
        public String name;
        public String url;

        public BankInfo() {
            name = "";
            url ="";
        }

        public String toString() {
            return "bank: " + name + " " + url;
        }
    }

    public int IIN;
    public int length;
    public String pay_system;
    public String type;
    public String brand;
    public String country;
    public BankInfo bank;

    public DatabaseEntry() {
        IIN = 0;
        length = 0;
        pay_system ="";
        type ="";
        brand = "";
        country = "";
        bank = null;
    }
    
    private String getHTMLTableLine(String line_name, String line_value) {
        if (line_value.equals("") || line_value.equals("-1")) line_value = "?";
        return "<tr><td>" + line_name + "</td><td>" + line_value + "</td></tr>";
    } 

    public String getHTMLTable() {
        return  "<table border=" + '"' + "1" + '"' + ">" +
                    getHTMLTableLine("IIN", String.valueOf(IIN)) +
                    getHTMLTableLine("length", String.valueOf(length)) +
                    getHTMLTableLine("pay_system", pay_system) +
                    getHTMLTableLine("type", type) +
                    getHTMLTableLine("country", country) +
                    getHTMLTableLine("bank name", bank.name) +
                    getHTMLTableLine("bank site url", bank.url) +
                "</table>";
    }

    public String toString() {
        return "IIN: " + IIN + " length: " + length + " pay_system: " + pay_system + " type: " + type + " brand: " + brand + " country: " + country + " " + bank;
    }
}
