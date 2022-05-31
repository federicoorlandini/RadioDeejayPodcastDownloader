import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.time.LocalDate;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.xml.sax.SAXException;


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

        var httpClient = HttpClient.newHttpClient();
        var dataExtractor = new DataExtractor(System.out, URL, httpClient);
        List<PodCastItem> filesToDownload = dataExtractor.GetItemsToDownload();

        var downloader = new Downloader();
        downloader.Download(filesToDownload, initialDate, maxNumberOfFiles, outputFolder);        
    }    
}
