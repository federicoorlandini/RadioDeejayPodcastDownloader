import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.*;


public class DeejayDownloader {
    private static final String URL = "https://deejayreloadedpodcast.maxxer.it/v2019/rss/deejay-chiama-italia.xml";
    private static LocalDate initialDate;
    private static int maxNumberOfFiles;
    private static String outputFolder;

    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException, ParserConfigurationException, SAXException {
        // Parse the args
        Options options = new Options();
        options.addOption("s", true, "The date of the older podcast to download (yyyy-mm-dd)");
        options.addOption("max", true, "The max number of files to download (integer)");
        options.addOption("d", true, "The folder path where store the files");

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine line = parser.parse(options, args);

            initialDate = LocalDate.parse(line.getOptionValue("s"));
            outputFolder = line.getOptionValue("d");
            maxNumberOfFiles = Integer.parseInt(line.getOptionValue("max"));
        }
        catch (ParseException ex) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("javac", options);
        }

        // Get the XML from the remote website
        System.out.printf("Getting the XML from %s%n", URL);
        var xml = GetXML();

        // Parse the XML and build the files collections
        System.out.println("Parsing....");
        List<PodCastItem> filesToDownload = GetFilesToDownload(xml);
        System.out.printf("Found %s items.%n", filesToDownload.size());

        // Sort the files to download by publication date
        filesToDownload.sort((left, right) -> {
            if( left.publicationDate() == right.publicationDate()) {
                return 0;
            }
            return left.publicationDate().isAfter(right.publicationDate()) ? -1 : 1;
        });

        // Filter for files that have been published before the date specified in the parameters
        filesToDownload = FilterOutOlderThan(filesToDownload, initialDate);

        // Filter the files based on the initialDate passed as parameter and the max number of files to download
        filesToDownload = GetLast(filesToDownload, maxNumberOfFiles);

        // Download the files
        for (var fileToDownload: filesToDownload) {
            var filename = GetFilenameFromUrl(fileToDownload.url());
            DownloadFile(fileToDownload.url(), filename, outputFolder);
        }
    }

    private static List<PodCastItem> FilterOutOlderThan(List<PodCastItem> list, LocalDate initialDate) {
        var filteredList = new ArrayList<PodCastItem>();

        for (var item : list) {
            if( item.publicationDate().isAfter(initialDate)) {
                filteredList.add(item);
            }
        }

        return filteredList;
    }

    private static List<PodCastItem> GetLast(List<PodCastItem> filesToDownload, int maxNumberOfFilesToTake) {
        var beginIndex = filesToDownload.size() - 1 - maxNumberOfFilesToTake;
        var endIndex = filesToDownload.size();
        return filesToDownload.subList(beginIndex, endIndex);
    }

    private static String GetFilenameFromUrl(String url) {
        // Split the string using the character '/' as separator and get the last part
        var parts = url.split("/");
        return parts[parts.length - 1];
    }

    private static String GetXML() throws URISyntaxException, IOException, InterruptedException {
        var httpClient = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder(new URI(URL)).GET().build();

        HttpResponse<String> response= httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    private static List<PodCastItem> GetFilesToDownload(String xml) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));
        doc.getDocumentElement().normalize();

        var itemNodes = doc.getDocumentElement().getElementsByTagName("item");

        var result = new ArrayList<PodCastItem>();

        for (var i = 0; i < itemNodes.getLength(); i++) {
            var itemNode = itemNodes.item(i);

            LocalDate publicationDate = null;
            String fileUrl = null;

            var itemNodeChildren = itemNode.getChildNodes();
            for(var j = 0; j < itemNodeChildren.getLength(); j++) {
                var childNode = itemNodeChildren.item(j);

                if( childNode.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                if(childNode.getNodeName().equals("pubDate")) {
                    publicationDate = LocalDateTime.parse(childNode.getFirstChild().getNodeValue(), DateTimeFormatter.RFC_1123_DATE_TIME).toLocalDate();
                } else if (childNode.getNodeName().equals("content:encoded")) {
                    fileUrl = childNode.getFirstChild().getNodeValue();
                }
            }

            if( publicationDate != null && fileUrl != null ) {
                var record = new PodCastItem(publicationDate, fileUrl);
                result.add(record);
            }
        }

        return result;
    }

    private static void DownloadFile(String url, String filename, String outputFolder) {
        // Check if the folder exists. If not, create it
        var folder = new File(outputFolder);
        if( !folder.exists() ) {
            System.out.printf("Folder %s doesn't exist. Creating it...%n", folder.getAbsolutePath());
            folder.mkdir();
        }

        System.out.printf("Downloading file %s ...%n", url);
        try(var inputStream = new BufferedInputStream(new URI(url).toURL().openStream());
            var fileOutputStream = new FileOutputStream(Path.of(outputFolder, filename).toString())) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while((bytesRead = inputStream.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
