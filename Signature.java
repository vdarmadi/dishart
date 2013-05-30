import org.apache.commons.codec.binary.Base64;

/*
 * Signs a URL path for getting a location.
 *
 * Usage:
 *
 * java -cp . Signature CLIENT_ID SIGNING_KEY LOCATION_ID
 */
public class Signature {

	public static void main(String[] args) throws java.security.SignatureException {
			String result;
            String clientId = args[0];
            String signKey = args[1];
			String locationId = args[2];
            if (signKey.length() % 4 != 0) {
                // The length of the signing key needs to be a multiple of 4
                for (int x = signKey.length() % 4; x  < 4; x++) {
                    signKey += "=";
                }
            }
            //String urlPath = "/locations/" + locationId + "/menu?client=" + clientId;
            //String urlPath = "/locations/search?q=94710&count=20&client=" + clientId;
            //String urlPath = "/locations/" + locationId + "?client=" + clientId;
            //String urlPath = "/locations/hana-japan-steakhouse/shortmenu?client=" + clientId;
            //String urlPath = "/locations/search?q=Haru&page=3&count=100&client=" + clientId;
            //String urlPath = "/locations/search?q=94609&page=0&count=100&client=" + clientId;
            //String urlPath = "/locations/pyung-chang-restaurante/menu?client=" + clientId;
            String urlPath = "/locations/addis-ethiopian-restaurant/menu?client=" + clientId;
            
            
			try {
				byte[] binaryKey = (new Base64()).decode(signKey.replaceAll("-", "\\+").replaceAll("_", "/"));

				javax.crypto.spec.SecretKeySpec signingKey = new javax.crypto.spec.SecretKeySpec(binaryKey, "HmacSHA1");
				javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
				mac.init(signingKey);

				byte[] rawHmac = mac.doFinal(urlPath.getBytes());

				// base64-encode the hmac and remove any trailing equals
				result = (new Base64()).encodeAsString(rawHmac).replaceAll("\\+", "-").replaceAll("/", "_");
                int firstEquals = result.indexOf("=");
                if (firstEquals > 0) {
                    result = result.substring(0, firstEquals);
                }

				System.out.println("http://api.singleplatform.co" + urlPath + "&sig=" + result);
			} catch (Exception e) {
				throw new java.security.SignatureException("Failed to generate HMAC : " + e.getMessage());
			}
		}

}

