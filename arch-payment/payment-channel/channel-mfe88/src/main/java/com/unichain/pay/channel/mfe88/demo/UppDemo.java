package com.unichain.pay.channel.mfe88.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.pisgah.gatewayeg.util.RSA;
import com.unichain.pay.channel.mfe88.utils.Base64;
import org.springframework.util.StringUtils;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class UppDemo {
    private static final String CHAR_ENCODING = "UTF-8";
    static String url = "http://localhost:8080/PayApi/";
    static String wechatUrl = "https://core.mch.weixin.qq.com/pay/unifiedorder";
    static BASE64Decoder decode = new BASE64Decoder();
    static BASE64Encoder ben = new BASE64Encoder();
    static ObjectMapper mapper = new ObjectMapper();
    static Map<String, Object> map = new HashMap<String, Object>();
    static Gson gson = new Gson();
    /**
     * 指定key的大小
     */
    private static int KEYSIZE = 1024;

    public static void main(String[] args) throws Exception {
    }


    public static String generateSign(String key, String content) {
        String sign = "";
        if (key.length() > 32) {
            try {
                System.out.println("rsa签串:" + content);
                sign = rsasign(content, key);
//				System.out.println("encode前:"+sign);
                sign = URLEncoder.encode(sign, "UTF-8");
//				System.out.println("encode后:"+sign);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            sign = md5UpperCase(content + key, "UTF-8");// 商户签名数据
            System.out.println("md5签串:" + content + key);
        }
//       System.out.println(sign);
        return sign;
    }

    public static boolean checkSign(String key, String content, String gotsign) {
        String sign = "";
        boolean flag = false;
        if (key.length() > 32) {
            try {
                flag = rsacheckSign(content, URLDecoder.decode(gotsign, "UTF-8"), key);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            System.out.println("rsa验证签名：" + flag);
        } else {
            sign = md5UpperCase(content + key, "UTF-8");// 商户签名数据
            flag = sign.equals(gotsign);
            System.out.println("md5验证签名：" + flag);
        }
        return flag;

    }

    public static String md5UpperCase(String inStr, String charset) {
        return md5(inStr, charset).toUpperCase();
    }

    public static String md5(String inStr, String charset) {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }

        byte[] md5Bytes;
        StringBuffer hexValue = new StringBuffer();
        try {
            md5Bytes = md5.digest(inStr.getBytes(charset));
            for (int i = 0; i < md5Bytes.length; i++) {
                int val = ((int) md5Bytes[i]) & 0xff;
                if (val < 16)
                    hexValue.append("0");
                hexValue.append(Integer.toHexString(val));
            }
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return hexValue.toString();
    }


    public static boolean notEmpty(String s) {
        return s != null && !"".equals(s) && !"null".equals(s);
    }

    public synchronized static String nextGeneratorOrderNo() {
        final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSS");
        final AtomicInteger count = new AtomicInteger(0);
        LocalDateTime localDateTime = LocalDateTime.now();
        if (count.getAndIncrement() == 10000) {
            count.set(0);
        }
        String orderNo = localDateTime.format(FORMATTER) + String.format("%04d", count.get());
        System.out.println("单号：" + orderNo);
        return orderNo;
    }

    public static String AESEncode(String encodeRules, String content) {
        try {
            // 1.构造密钥生成器，指定为AES算法,不区分大小写
            KeyGenerator keygen = KeyGenerator.getInstance("AES");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.setSeed(encodeRules.getBytes());
            // 2.根据ecnodeRules规则初始化密钥生成器
            // 生成一个128位的随机源,根据传入的字节数组
            keygen.init(128, random);
            // 3.产生原始对称密钥
            SecretKey original_key = keygen.generateKey();
            // 4.获得原始对称密钥的字节数组
            byte[] raw = original_key.getEncoded();
            // 5.根据字节数组生成AES密钥
            SecretKey key = new SecretKeySpec(raw, "AES");
            // 6.根据指定算法AES自成密码器
            Cipher cipher = Cipher.getInstance("AES");
            // 7.初始化密码器，第一个参数为加密(Encrypt_mode)或者解密解密(Decrypt_mode)操作，第二个参数为使用的KEY
            cipher.init(Cipher.ENCRYPT_MODE, key);
            // 8.获取加密内容的字节数组(这里要设置为utf-8)不然内容中如果有中文和英文混合中文就会解密为乱码
            byte[] byte_encode = content.getBytes(StandardCharsets.UTF_8);
            // 9.根据密码器的初始化方式--加密：将数据加密
            byte[] byte_AES = cipher.doFinal(byte_encode);
            // 10.将加密后的数据转换为字符串
            // 这里用Base64Encoder中会找不到包
            // 解决办法：
            // 在项目的Build path中先移除JRE System Library，再添加库JRE System
            // Library，重新编译后就一切正常了。
            String AES_encode = new BASE64Encoder().encode(byte_AES);
            // 11.将字符串返回
            return AES_encode;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }

        // 如果有错就返加nulll
        return null;
    }

    public static String AESDncode2(String content, String encodeRules) {
        try {
            // 1.构造密钥生成器，指定为AES算法,不区分大小写
            KeyGenerator keygen = KeyGenerator.getInstance("AES");

            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.setSeed(encodeRules.getBytes());
            // 2.根据ecnodeRules规则初始化密钥生成器
            // 生成一个128位的随机源,根据传入的字节数组
            keygen.init(128, random);
            // 3.产生原始对称密钥
            SecretKey original_key = keygen.generateKey();
            // 4.获得原始对称密钥的字节数组
            byte[] raw = original_key.getEncoded();
            // 5.根据字节数组生成AES密钥
            SecretKey key = new SecretKeySpec(raw, "AES");
            // 6.根据指定算法AES自成密码器
            Cipher cipher = Cipher.getInstance("AES");
            // 7.初始化密码器，第一个参数为加密(Encrypt_mode)或者解密(Decrypt_mode)操作，第二个参数为使用的KEY
            cipher.init(Cipher.DECRYPT_MODE, key);
            // 8.将加密并编码后的内容解码成字节数组
            byte[] byte_content = new BASE64Decoder().decodeBuffer(content);
            /*
             * 解密
             */
            byte[] byte_decode = cipher.doFinal(byte_content);
            String AES_decode = new String(byte_decode, StandardCharsets.UTF_8);
            return AES_decode;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        // 如果有错就返加nulll
        return null;
    }

    public static String rsasign(String content, String privateKey) {
        String charset = CHAR_ENCODING;
        try {
            PKCS8EncodedKeySpec priPKCS8 = new PKCS8EncodedKeySpec(Base64.decodeBase64(privateKey.getBytes()));
            KeyFactory keyf = KeyFactory.getInstance("RSA");
            PrivateKey priKey = keyf.generatePrivate(priPKCS8);

            Signature signature = Signature.getInstance("SHA1WithRSA");

            signature.initSign(priKey);
            signature.update(content.getBytes(charset));

            byte[] signed = signature.sign();

            return new String(Base64.encodeBase64(signed));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static boolean rsacheckSign(String content, String sign, String publicKey) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            byte[] encodedKey = Base64.decode2(publicKey);
            PublicKey pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(encodedKey));

            Signature signature = Signature.getInstance("SHA1WithRSA");

            signature.initVerify(pubKey);
            signature.update(content.getBytes(StandardCharsets.UTF_8));

            boolean bverify = signature.verify(Base64.decode2(sign));
            return bverify;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static Map<String, Object> generateKeyPair() throws Exception {
        /** RSA算法要求有一个可信任的随机数源 */
        SecureRandom sr = new SecureRandom();
        /** 为RSA算法创建一个KeyPairGenerator对象 */
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        /** 利用上面的随机数据源初始化这个KeyPairGenerator对象 */
        kpg.initialize(KEYSIZE, sr);
        /** 生成密匙对 */
        KeyPair kp = kpg.generateKeyPair();
        /** 得到公钥 */
        Key publicKey = kp.getPublic();
        byte[] publicKeyBytes = publicKey.getEncoded();
        String pub = new String(Base64.encodeBase64(publicKeyBytes),
                CHAR_ENCODING);
        /** 得到私钥 */
        Key privateKey = kp.getPrivate();
        byte[] privateKeyBytes = privateKey.getEncoded();
        String pri = new String(Base64.encodeBase64(privateKeyBytes),
                CHAR_ENCODING);

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("publicKey", pub);
        map.put("privateKey", pri);
        RSAPublicKey rsp = (RSAPublicKey) kp.getPublic();
        BigInteger bint = rsp.getModulus();
        byte[] b = bint.toByteArray();
        byte[] deBase64Value = Base64.encodeBase64(b);
        String retValue = new String(deBase64Value);
        map.put("modulus", retValue);
        return map;
    }

    public static boolean isEmpty(String s) {
        return s == null || "".equals(s) || "null".equals(s);
    }

    public static String obj2str(Object s) {
        if (s == null) {
            return "";
        } else if (isEmpty(s.toString())) {
            return "";
        } else {
            return s.toString();
        }
    }

    public static boolean verifySign(String sign, String publicKey, String singStr) {
        boolean verifySign = false;
        try {
            System.out.print("原串:" + singStr);
            verifySign = RSA.verifySign(singStr, sign, publicKey, "UTF-8");
        } catch (Exception e) {
            System.out.println("验签异常:" + singStr);
            e.printStackTrace();
        }
        return verifySign;
    }

    public static Map<String, Object> paraFilter(Map<String, Object> sArray) {
        Map<String, Object> result = new HashMap<String, Object>();
        String value = null;
        if (sArray == null || sArray.size() <= 0) {
            return result;
        }
        for (String key : sArray.keySet()) {
            value = sArray.get(key) + "";
            if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value) || key.equalsIgnoreCase("sign")) {
                // || key.equalsIgnoreCase("signType")) { //signType
                continue;
            }
            result.put(key, value);
        }
        return result;
    }

    static class DarKnight {

        private static final String ALGORITHM = "AES";

        private static final byte[] SALT = "tHeApAcHe6410111".getBytes();// THE KEY MUST BE SAME
        private static final String X = DarKnight.class.getSimpleName();

        static String getEncrypted(String plainText) {

            if (plainText == null) {
                return null;
            }

            Key salt = getSalt();

            try {
                Cipher cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(Cipher.ENCRYPT_MODE, salt);
                byte[] encodedValue = cipher.doFinal(plainText.getBytes());
                return Base64.encode(encodedValue.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }

            throw new IllegalArgumentException("Failed to encrypt data");
        }

        public static String getDecrypted(String encodedText) {

            if (encodedText == null) {
                return null;
            }

            Key salt = getSalt();
            try {
                Cipher cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, salt);
                String decodedValue = Base64.decode(encodedText);
                byte[] decValue = cipher.doFinal(decodedValue.getBytes());
                return new String(decValue);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        static Key getSalt() {
            return new SecretKeySpec(SALT, ALGORITHM);
        }

    }

}
