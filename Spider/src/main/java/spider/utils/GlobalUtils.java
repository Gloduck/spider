package spider.utils;

import java.io.File;
import java.util.Objects;

/**
 * @author Gloduck
 */
public class GlobalUtils {
    private GlobalUtils(){}
    public static File getCurrentPathFile(){
        String path = GlobalUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        File file = new File(path);
        return file.getParentFile();
    }
    public static File getFileInJarPath(String fileName){
        File currentPathFile = getCurrentPathFile();
        File[] files = currentPathFile.listFiles((dir, name) -> {
            return Objects.equals(name, fileName);
        });
        return files.length == 1 ? files[0] : null;
    }
}
