import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Downloader {
    public void Download(List<PodCastItem> filesToDownload, LocalDate initialDate, int maxNumberOfFiles, String outputFolder) {
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

    private List<PodCastItem> FilterOutOlderThan(List<PodCastItem> list, LocalDate initialDate) {
        var filteredList = new ArrayList<PodCastItem>();

        for (var item : list) {
            if( item.publicationDate().isAfter(initialDate)) {
                filteredList.add(item);
            }
        }

        return filteredList;
    }

    private List<PodCastItem> GetLast(List<PodCastItem> filesToDownload, int maxNumberOfFilesToTake) {
        var beginIndex = filesToDownload.size() - 1 - maxNumberOfFilesToTake;
        var endIndex = filesToDownload.size();
        return filesToDownload.subList(beginIndex, endIndex);
    }

    private String GetFilenameFromUrl(String url) {
        // Split the string using the character '/' as separator and get the last part
        var parts = url.split("/");
        return parts[parts.length - 1];
    }

    private void DownloadFile(String url, String filename, String outputFolder) {
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
