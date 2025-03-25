package com.atsuishio.superbwarfare.client.screens.modsell;

import net.minecraft.client.Minecraft;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

public class TranslationRecord {

    private static final String KEY = "26342ea7c1f2d105678e702dd95f3285c5e1e4ff18814f5f74946e4f02cfdc78";
    public static final Map<String, String> CONTENT = Map.ofEntries(
            Map.entry("zh_cn", "f07591350fc89b0ff0f7ddc537fefec2f62cbc5e80ea77467b09899754cd54067dea26eef69388d0eef680add46073380891915bbb53ac6902c8566d1ea6679652849a70b6e003e2d4c97efd714b4919bee266a8813aca3d9741dbbc0186a41d420aa3f5d0960ab3c01767f0d79eb06b58d019a3e6cdb94f07f5106c16da8ea19c4327448ac68b446ffeaae9dbd8be844d5858792b7ef2c2686205bde67726df54d05ba4d08481cd997e4d7bf001b2034f4748b5c05637d400bbd1797df3d7c13c45763868bc0261309d701fbd4d8dabe0fd5336d13cbc727bd2ba27fe958df77c9e0deb19f870561a27bce013645b1a825d05f2530680ace33b020cd3f3e7772a70758bfdd2fbdf88351c3e9db6a1dd9a4679ad402dea8bdd0566c4d9fe0a11"),
            Map.entry("en_us", "bdff19c5a3d29d85fecfe240f801c08ff6f9259aca7a8700ef6a03801a48b21b83420177692c88cc280378a755d3d44dc21ac1a757f96a5a4bc281a3c75e9fcb64f64196d59e81f04815e3b4ff83e3ac0c8dfbbb28c6731f9c468018e5e3e9460ad7e5bd1fd2edfbaa12ea22e53d1ea41733c933af7a5af79a670d5927eb4e9a37c0a9302b998aecbd9988209ce63882f4f4f0834d1e9abcc67ca99735261ef810df421d3b87b7a361b36c7771c15e64d17c7ca748061f283723b0f13230f6c3e1f6542507f4808350c2a36ff2822449d06f42bf6a8cbd29629e3e4b026a70fc")
    );
    public static final Map<String, String> TITLE = Map.ofEntries(
            Map.entry("zh_cn", "f47aedcdf60b4ca63eed627b93e3d8d873638f82479c7ca3846b3f1939ad7e2f"),
            Map.entry("en_us", "5630e4b7145bd2c4b748077389bb1bf041b0a2c6b5147af89917f50af2ac3041")
    );

    public static final Map<String, String> CHECK = Map.ofEntries(
            Map.entry("zh_cn", "f1eb77633be9e73e04e6f3452d3869fa2b849438b1e0bf738d61452c182c5e26c9d86d259e20a88505ad262d9daf8c1d"),
            Map.entry("en_us", "543f8d6c4c8ba78365cf0fde9f4fe52f18800ac0ad501e743372134884e3bc9f9f6b451d0376b3997d5c2159e6831d3a83d6b3ce6b4d5749cba96b894d2a4d275a882e9ef1e9f912c35ee3288814f9af")
    );

    public static String get(Map<String, String> map) {
        try {
            var selectedLanguage = Minecraft.getInstance().getLanguageManager().getSelected();

            var encryptedContent = map.get(selectedLanguage);
            if (encryptedContent == null) {
                encryptedContent = map.get("en_us");
            }

            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(HexFormat.of().parseHex(KEY), "AES"));
            byte[] encrypted = cipher.doFinal(HexFormat.of().parseHex(encryptedContent));
            return new String(encrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            throw new IllegalStateException("SuperbWarfare warning screen translation broken, exiting");
        }
    }
}
