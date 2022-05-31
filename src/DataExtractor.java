import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/* This class retrieve the XML that lists all the podcasts, parse the XML and provide
   a list of podcast that can be downloaded */
public class DataExtractor {

    private final PrintStream _out;
    private final URI _url;
    private final HttpClient _httpClient;

    public DataExtractor(PrintStream out, String url, HttpClient httpClient) throws URISyntaxException {
        _out = out;
        _url = new URI(url);
        _httpClient = httpClient;
    }

    public List<PodCastItem> GetItemsToDownload() throws ParserConfigurationException, IOException, SAXException, InterruptedException {
        // Get the XML from the remote website
        _out.printf("Getting the XML from %s%n", _url);
        var xml = GetXML();

        // Parse the XML and build the files collections
        _out.println("Parsing....");
        List<PodCastItem> filesToDownload = GetFilesToDownload(xml);
        _out.printf("Found %s items.%n", filesToDownload.size());

        return filesToDownload;
    }

    private String GetXML() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(_url).GET().build();

        HttpResponse<String> response= _httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    private List<PodCastItem> GetFilesToDownload(String xml) throws ParserConfigurationException, IOException, SAXException {
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
}
