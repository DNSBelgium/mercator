//import java.sql.*;
//import be.dnsbelgium.mercator.common.messaging.idn.IDN2008;
//
//public class DomainNameConvertion {
//    /*
//     * need privileges to create a new column, read and update the db
//     */
//    //    TODO har, html & screenshot key still have old label
//
////  content_crawler.content_crawl_result, dispatcher.dispatcher_event, dns_crawler.request, smpt_crawler.smpt_crawl_result, vat_crawler.vat_crawl_result, feature_extraction.html_features
//    static String tableName = "content_crawler.content_crawl_result";
//    private static final String DB_URL = "jdbc:postgresql://localhost:5432/postgres";
//    private static final String DB_USER = "postgres";
//    private static final String DB_PASSWORD = "password";
//
//    public static Connection getConnection() throws SQLException {
//        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
//    }
//
//    public static void CreateNewColumnConvertedDomainNames(String tableName) {
//        try (Connection connection = getConnection()) {
//            String newColumForConvertedValues = String.format("ALTER TABLE %s ADD COLUMN IF NOT EXISTS converted_domain_names VARCHAR(255);",tableName);
//            Statement statement = connection.createStatement();
//            statement.execute(newColumForConvertedValues);
//            statement.close();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public static void GetALabelDomainNamesAndConvert(String tableName) {
//        try (Connection connection = getConnection()) {
//            String selectQuery = String.format("SELECT * FROM %s WHERE domain_name LIKE 'xn--%%';", tableName);
//            Statement statement = connection.createStatement();
//            ResultSet resultSet = statement.executeQuery(selectQuery);
//
//            System.out.println("resultSet: "+resultSet);
//
//            while (resultSet.next()) {
//                String punycode = resultSet.getString("domain_name");
//                System.out.println("punycode: "+punycode);
//                String unicode = convertPunycodeToUnicode(punycode);
//                System.out.println("unicode: "+unicode);
//                updateUnicodeValue(connection, punycode, unicode, tableName);
//            }
//
//            resultSet.close();
//            statement.close();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private static String convertPunycodeToUnicode(String punycode) {
//        return IDN2008.toUnicode(punycode);
//    }
//
//    private static void updateUnicodeValue(Connection connection, String punycode, String unicode, String tableName) throws SQLException {
//        String updateQuery = String.format("UPDATE %s SET converted_domain_names = ? WHERE domain_name = ?;",tableName);
//        PreparedStatement preparedStatement = connection.prepareStatement(updateQuery);
//        preparedStatement.setString(1, unicode);
//        preparedStatement.setString(2, punycode);
//        preparedStatement.executeUpdate();
//        preparedStatement.close();
//    }
//
//    public static void main(String[] args){
//        CreateNewColumnConvertedDomainNames(tableName);
//        GetALabelDomainNamesAndConvert(tableName);
//    }
//}
