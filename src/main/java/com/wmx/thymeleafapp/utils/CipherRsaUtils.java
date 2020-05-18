package com.wmx.thymeleafapp.utils;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * RSA 非对称加密工具类。
 * 1、公钥（PUBLIC_KEY）、私钥：PRIVATE_KEY 必须分开使用，比如公钥加密时，必须是私钥解密，反之私钥加密时，必须是公钥解密
 *
 * @author wangmaoxiong
 * @version 1.0
 * @date 2020/5/18 15:41
 */
public class CipherRsaUtils {

    /**
     * CIPHER_TRANSFORMS : cipher 实例化时的 加密算法/反馈模式/填充方案。ECB 表示无向量模式
     * ALGORITHM: 创建密钥时使用的算法
     * KEY_PAIR_LENGTH: 秘钥对长度。数值越大，能加密的内容就越大。
     * <p>
     * 如 KEY_PAIR_LENGTH 为 1024 时加密数据的长度不能超过 117 字节
     * 如 KEY_PAIR_LENGTH 为 2048 时加密数据的长度不能超过 245 字节
     * 依次类推
     * </p>
     */
    private static final String CIPHER_TRANSFORMS = "RSA/ECB/PKCS1Padding";
    private static final int KEY_PAIR_LENGTH = 1024;

    /**
     * 生成 RSA 密钥对：公钥（PUBLIC_KEY）、私钥：PRIVATE_KEY
     *
     * @param secureRandomSeed ：SecureRandom 随机数生成器的种子，只要种子相同，则生成的公钥、私钥就是同一对.
     *                         <p>randomSeed 长度可以自定义，加/解密必须是同一个.</p>
     * @return
     */
    public static KeyPair generateRsaKeyPair(String secureRandomSeed) {
        //KeyPair 是密钥对（公钥和私钥）的简单持有者。加密、解密都需要使用.
        KeyPair keyPair = null;
        try {
            //获取生成 RSA 加密算法的公钥/私钥对 KeyPairGenerator 对象
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            //获取实现指定算法（SHA1PRNG）的随机数生成器（RNG）对象.
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            //重新设定此随机对象的种子.
            secureRandom.setSeed(secureRandomSeed.getBytes());
            /**
             * initialize(int keySize, SecureRandom random):使用给定的随机源（random）初始化特定密钥大小的密钥对生成器。
             * keySize: 健的大小值，这是一个特定于算法的度量。值越大，能加密的内容就越多，否则会抛异常：javax.crypto.IllegalBlockSizeException: Data must not be longer than xxx bytes
             * 如 keySize 为 2048 时加密数据的长度不能超过 245 字节。
             */
            keyPairGenerator.initialize(KEY_PAIR_LENGTH, secureRandom);
            //genKeyPair(): 生成密钥对
            keyPair = keyPairGenerator.genKeyPair();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return keyPair;
    }

    /**
     * 使用私钥（PrivateKey）对数据进行加密 或 解密
     *
     * @param content          :待加/解密的数据。如果待解密的数据，则是 Base64 编码后的可视字符串.
     * @param model            ：1 表示加密模式（Cipher.ENCRYPT_MODE），2 表示解密模式（Cipher.DECRYPT_MODE）
     * @param secureRandomSeed ：SecureRandom 随机数生成器的种子，只要种子相同，则生成的公钥、私钥就是同一对.
     *                         加解密必须是同一个.
     * @return ：返回加/解密后的数据，如果是加密，则将字节数组使用 Base64 转为可视字符串.
     */
    public static String cipherByPrivateKey(String content, int model, String secureRandomSeed) {
        String result = "";
        try {
            //获取 RSA 密钥对
            KeyPair keyPair = generateRsaKeyPair(secureRandomSeed);
            /**getPrivate()：获取密钥对的私钥。
             * getEncoded(): 返回已编码的密钥，如果密钥不支持编码，则返回 null*/
            byte[] privateEncoded = keyPair.getPrivate().getEncoded();
            /**PKCS8EncodedKeySpec(byte[] encodedKey): 使用给定的编码密钥创建新的 PKCS8EncodedKeySpec
             * PKCS8EncodedKeySpec 表示 ASN.1 号私钥的编码*/
            EncodedKeySpec encodedKeySpec = new PKCS8EncodedKeySpec(privateEncoded);
            //创建 KeyFactory 对象，用于转换指定算法(RSA)的公钥/私钥。
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            //从提供的密钥规范生成私钥对象
            PrivateKey keyPrivate = keyFactory.generatePrivate(encodedKeySpec);
            //实例化 Cipher 对象。
            result = encryptionDecryption(content, model, keyPrivate);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 使用公钥（PublicKey）对数据进行加密 或 解解
     *
     * @param content          :待加解密的数据
     * @param model            ：1 表示加密模式（Cipher.ENCRYPT_MODE），2 表示解密模式（Cipher.DECRYPT_MODE）
     * @param secureRandomSeed ：SecureRandom 随机数生成器的种子，只要种子相同，则生成的公钥、私钥就是同一对.
     *                         加解密必须是同一个.
     * @return :返回加密 或者 解密后的数据
     */
    public static String cipherByPublicKey(String content, int model, String secureRandomSeed) {
        String result = "";
        try {
            // 得到公钥
            KeyPair keyPair = generateRsaKeyPair(secureRandomSeed);
            EncodedKeySpec keySpec = new X509EncodedKeySpec(keyPair.getPublic().getEncoded());
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey keyPublic = keyFactory.generatePublic(keySpec);
            // 数据加/解密
            result = encryptionDecryption(content, model, keyPublic);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 加密或解密
     *
     * @param content :待加解密的数据
     * @param model   ：1 表示加密模式（Cipher.ENCRYPT_MODE），2 表示解密模式（Cipher.DECRYPT_MODE）
     * @param key     ：公钥（PUBLIC_KEY）或 私钥（PRIVATE_KEY）的 key
     * @return
     */
    private static String encryptionDecryption(String content, int model, Key key) {
        String result = "";
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMS);
            //init(int opMode, Key key)：初始化 Cipher.
            cipher.init(model, key);
            //如果是解密模式，则需要使用 Base64 进行解码.
            byte[] contentBytes = content.getBytes();
            if (model == Cipher.DECRYPT_MODE) {
                contentBytes = new BASE64Decoder().decodeBuffer(content);
            }
            /**执行加密 或 解密操作。如果 contentBytes 内容过长，则 doFinal 可能会抛出异常.
             *javax.crypto.IllegalBlockSizeException: Data must not be longer than 245 bytes ：数据不能超过xxx字节
             * 此时需要调大 KEY_PAIR_LENGTH 的值*/
            byte[] finalBytes = cipher.doFinal(contentBytes);
            //如果是加密，则将加密后的字节数组使用 Base64 转成可视化的字符串。否则是解密时，直接 new String 字符串.
            if (model == Cipher.ENCRYPT_MODE) {
                result = new BASE64Encoder().encode(finalBytes);
            } else if (model == Cipher.DECRYPT_MODE) {
                result = new String(finalBytes);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static void main(String[] args) {
        String content = "红网时刻5月17日讯（记者 汪衡 通讯员 颜雨彬 汪丹）“场面壮观，气势磅礴.";
        String secureRandomSeed = "TPCz0lDvTHybGMsHTJi3mJ7Pt48llJmRHb";

        System.out.println("被加密数据字节大小：" + content.getBytes().length + " 字节，" + content.length() + " 个字符");
        System.out.println("源内容：\n" + content);
        try {
            //公钥、私钥必须分开使用，公钥解密时，必须是私钥解密，反之亦然.
            String encrypted = CipherRsaUtils.cipherByPublicKey(content, 1, secureRandomSeed);
            String decrypted = CipherRsaUtils.cipherByPrivateKey(encrypted, 2, secureRandomSeed);
            System.out.println("加密后：\n" + encrypted);
            System.out.println("解密后：\n" + decrypted);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
