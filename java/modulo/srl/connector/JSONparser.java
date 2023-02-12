package modulo.srl.connector;

// Compacted version of KasparNagu/plain-java-json
// See https://github.com/KasparNagu/plain-java-json

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;


public class JSONparser {

    private Map<?, ?> parsedMap;
	private Boolean parsed;

    public JSONparser(String jsonContent) {
		try {
			Object o = JSONParser.parseJSON(jsonContent);

			this.parsedMap = ((Map<?, ?>)o);
			this.parsed = true;
		} catch (JSONParser.JSONParseException e) {
			this.parsed = false;
		}
    }

    public String getString(String key) {
		if (!this.parsed)
			return null;

        return (String)this.parsedMap.get(key);
    }

    public static String toJSON(Object o) {
		String s;

		try {
			s = JSONSerializer.serializeJSON(o);
		} catch (JSONSerializer.JSONSerializerException e) {
			s = "";
		}

		return s;
    }
}


class JSONParser {
	public static class JSONParseException extends Exception {
		private static final long serialVersionUID = 7078320816065460L;

		public JSONParseException(String cause) {
			super(cause);
		}
	}
	public static Object parseJSONFile(String file) throws JSONParseException, IOException{
		return parseJSON(Paths.get(file));
	}
	public static Object parseJSON(String text) throws JSONParseException{
		return parseJSON(new Scanner(text));
	}
	public static Object parseJSON(Path file) throws JSONParseException, IOException{
		return parseJSON(new Scanner(file));
	}
	public static Object parseJSON(Scanner s) throws JSONParseException{
		Object ret = null;
		skipWhitespace(s);
		if(s.findWithinHorizon("\\{", 1) != null){
			HashMap<Object, Object> retMap = new HashMap<>();
			ret = retMap;
			skipWhitespace(s);
			if(s.findWithinHorizon("\\}", 1) == null){
				while(s.hasNext()){
					Object key = parseJSON(s);
					skipWhitespace(s);
					if(s.findWithinHorizon(":", 1) == null){
						fail(s,":");
					}
					Object value = parseJSON(s);
					retMap.put(key, value);
					skipWhitespace(s);
					if(s.findWithinHorizon(",", 1)== null){
						break;
					}
				}			
				if(s.findWithinHorizon("\\}", 1) == null){
					fail(s,"}");
				}
			}
		}else if(s.findWithinHorizon("\"", 1) != null){
			ret = s.findWithinHorizon("(\\\\\\\\|\\\\\"|[^\"])*",0)
					.replace("\\\\", "\\")
					.replace("\\\"","\"");
			if(s.findWithinHorizon("\"", 1) == null){
				fail(s,"quote");
			}
		}else if(s.findWithinHorizon("'", 1) != null){
			ret = s.findWithinHorizon("(\\\\\\\\|\\\\'|[^'])*",0);
			if(s.findWithinHorizon("'", 1) == null){
				fail(s,"quote");
			}		
		}else if(s.findWithinHorizon("\\[", 1) != null){
			ArrayList<Object> retList = new ArrayList<>();
			ret = retList;
			skipWhitespace(s);
			if(s.findWithinHorizon("\\]", 1) == null){
				while(s.hasNext()){
					retList.add(parseJSON(s));
					skipWhitespace(s);
					if(s.findWithinHorizon(",", 1)== null){
						break;
					}
				}
				if(s.findWithinHorizon("\\]", 1) == null){
					fail(s,", or ]");
				}
			}
		}else if(s.findWithinHorizon("true",4) != null){
			ret = true;
		}else if(s.findWithinHorizon("false",5) != null){
			ret = false;
		}else if(s.findWithinHorizon("null",4) != null){
			ret = null;
		}else{
			String numberStart = s.findWithinHorizon("[-0-9+eE]", 1);
			if(numberStart != null){
				String numStr = numberStart + s.findWithinHorizon("[-0-9+eE.]*", 0);
				if(numStr.contains(".") | numStr.contains("e")){
					ret = Double.valueOf(numStr);
				}else{
					ret = Long.valueOf(numStr);
				}
			}else{
				throw new JSONParseException("No JSON value found. Found: " + s.findWithinHorizon(".{0,5}", 5));
			}
		}
		return ret;
	}
	private static void fail(Scanner scanner, String expected) throws JSONParseException {
		throw new JSONParseException("Expected " + expected +  " but found:" + scanner.findWithinHorizon(".{0,5}", 5));
	}
	private static void skipWhitespace(Scanner s) {
		s.findWithinHorizon("\\s*", 0);
	}
}

class JSONSerializer {
	public static class JSONSerializerException extends Exception {
		private static final long serialVersionUID = -4942348678107203496L;

		public JSONSerializerException(String cause) {
			super(cause);
		}
	}
	public static String serializeJSON(Object o) throws JSONSerializerException{
		StringWriter wrt = new StringWriter();
		try {
			serializeJSON(o,wrt);
		} catch (IOException e) {
			throw new JSONSerializerException("IOException during serialization");
		}
		return wrt.toString();
	}
	public static void serializeJSON(Object o, Writer w) throws JSONSerializerException, IOException {
		if(o == null){
			w.write("null");
		}else if(o instanceof String){
			w.write("\"");
			w.write(((String)o).replace("\\","\\\\").replace("\"", "\\\""));
			w.write("\"");			
		}else if(o instanceof Integer){
			w.write(Integer.toString((Integer)o));
		}else if(o instanceof Long){
			w.write(Long.toString((Long)o));
		}else if(o instanceof Double){
			w.write(Double.toString((Double)o));
		}else if(o instanceof Boolean){
			w.write(Boolean.toString((Boolean)o));
		}else if(o instanceof List){
			boolean first = true;
			w.write("[");
			for(Object l:(List<?>)o){
				if(first){
					first = false;
				}else{
					w.write(",");
				}
				serializeJSON(l, w);
			}
			w.write("]");
		}else if(o instanceof Map){
			boolean first = true;
			w.write("{");
			for(Map.Entry<?,?> l:((Map<?,?>)o).entrySet()){
				if(first){
					first = false;
				}else{
					w.write(",");
				}
				serializeJSON(l.getKey(), w);
				w.write(":");
				serializeJSON(l.getValue(), w);
			}
			w.write("}");
		}else{
			throw new JSONSerializerException("Can not serialize type:"+o.getClass().getSimpleName());
		}
	}
	
}
