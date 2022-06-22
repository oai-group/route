package apps.smartfwd.src.main.java.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.net.Link;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class JSONUtils {
    public static LinkedList<LinkedList<Integer>> convertToRoutes(String payload) throws JsonProcessingException {
            JsonNode jsonNode=new ObjectMapper().readTree(payload);
            LinkedList<LinkedList<Integer>> res=new LinkedList<>();
            
            return res;
    }
}
