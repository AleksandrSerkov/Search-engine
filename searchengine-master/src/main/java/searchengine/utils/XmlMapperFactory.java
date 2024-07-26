package searchengine.utils;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
public class XmlMapperFactory {
    // Метод для создания и возвращения нового XmlMapper
    public static XmlMapper createXmlMapper() {
        return new XmlMapper();
    }
}

