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
            Map.entry("en_us", "51e53d8ff9d843f158ee5968dc1a53500772f2b386602c53a1f492a24b7096db8ee8a83ba9e2871f6607d02b80ce33127956e88c85e055d8e23f791c0a7759e9704633f40c395575c46191f6363b548b5973d4a418b462177d5dc872ee4a4a2f146a796c92d0723437b2abf49c68902dc6b4d2f0bba4ff5d91c1f993f2ca112914cd967ed08569841f2419c47bd79083b104e9a4e02df81cae4f25a46b29481f51e67a5c4055cf8bf0c0fb767e6ecd9dd4c4c960355623a558f715fccf58a1a47d7d973d2419b15cf56781ff8ab1bc945bc5f5d8531e9fedba25fe77254549125e32c189eacaa2f4724012a5e8228c4b"),
            Map.entry("ru_ru", "5ce4194061fe1efffb7e2f564a483a51cc74ec4b7db99e6808076806e3506d3911af2c5a4936e895a63367543d8eec468ae3faf3b0df581ffbc92e070294953f61f2f74a9ef7561d10e3de57fd0e684bd69216c41e6b6a02e1563f3d79e1004aede4d1195769a737d05ca7711e9403f726f6873e6167a3118c38c5fd43043eb95352bd235d0a81650874734e8acb1d7bf15401d8017fe1001a1eb6e8c27753d1aec7f6b70c615f206d7400d5f69ae694fa852ca84f5494ffb524d0c2cd5f6f7106fd7f88c2353bc8b9470e28eb208aa793177d1d3e4e93fa711da5d4a1938f7ae4a18a2a365d273f768976e7e5134417aed6b05a7f3623789acbad9476b499445acebac62b2563d96c1ee5f02fb0c264f7bda12de0806a8011235269ea7397329c2905caeef506447c8ae3781541c07ae11d8f1d678f81e4e96f9c80c9492837ff63d7a8a92def9b21502dbcf307ee4a07b65cb88fda476d029a0790212db9541040888a2a8ba9c33758f95d6bb449a98792ef8fe66f9b0999005f6be929eaa50ed26994b1192fff0864804e82067d389703ebb8818929eeba4119d472dd725e48dc3a331eb16a643450a91e6c4c55c004bdcf526ff392366e11c412af9ad7545d68f1e2a9283fbb9dbcfc4fa489b8751d7fcd60323b395dd39335cc8983aa4f4ffd4eba3e23a511b9fbd4e5b78b6ff1b0cbb15fbd4cfdf46d3e3739b88e9b41334359b924fc2bba1447052120873140"),
            Map.entry("es_es", "5ce62032a0681b393d8c89d637eb4c209c2bbc7fa4b717403a2f5a796a12a459113382b5295afb60ce36240525fef46d1e063a9460312df6739f4f13caa7876929a448b448bf6570f8d19b4d8cdb99fc19771db7fd8734dcb96603324934d548002d1daaec8418e86955a5cb5d78f5e63dbfc6f98362d01cac0a475ea32328d4516bc448daed127fe7111acbae1a2de06be4883e08ed274314a9a6a1d8e9b3e3eefa6ede69b22d536c17c885ab59af30a8799688823c70ee35fb4833f6790663ea76b4d06824a2c7c67961067fa1577eb324da7d1e6d1fb4bdddcd826db82d091e41f816e5ed82163e3da3780c54d6cf817dcae427473c3e872e1950725af91461ce611cede9424f2f40211bcc7e9e65841e493e04fab031606a5dde48795fe8"),
            Map.entry("ja_jp", "7f46144cf5312e3f12fc3ce7e8cb42c56b3a58905f8304f393ad59631e14ad4481f62ccdb7de842e01f36088c2e5926ba2980fd7b483006fb6caa921e4dddd5f696d27f72b005d51de7dfeeaf38d18704c1f4f042264e5865b99d85c0462c5fcf35286231f0f0ee690afb978e4342e6c7e7db49f9c9da6e6c8e6cb097eb76e1a0a14b35b0cabda58a6e705dca4cece0ad00a9e9edb5a1c4c42be7504dbc024201d57e4fee550d67149cc415975f559e9587ec671cff67e9af5ce2aec5d995d30450e230706c525df863cd7a71932b2346f672e6f4c829acf6aa360c330bd9a5f1a93f6c153ae5c9e76094e10f2527e0a26b5b60280227a561020d5f31ab168d596cfc6a46e4b9fac69b2678c47c163ebb463c74805aa89261e1e7d6f332520dd4ef81c379f5c1914dae4d3bd0db57ac3abcbd417e3f7a118988832b3e19905e043a998f35a3a2bc9dd17c9879389251f")
    );
    public static final Map<String, String> TITLE = Map.ofEntries(
            Map.entry("zh_cn", "f47aedcdf60b4ca63eed627b93e3d8d873638f82479c7ca3846b3f1939ad7e2f"),
            Map.entry("en_us", "5630e4b7145bd2c4b748077389bb1bf041b0a2c6b5147af89917f50af2ac3041"),
            Map.entry("ru_ru", "7fd536c10691830c453ec6cbabdb6b5d6963c2036f0474cc5e40780ed25e31a0bdd82e73571e351a7d7478a6151a8eeb"),
            Map.entry("es_es", "5630e4b7145bd2c4b748077389bb1bf041b0a2c6b5147af89917f50af2ac3041"),
            Map.entry("ja_jp", "5630e4b7145bd2c4b748077389bb1bf08718c777cd38cf6a35916a05f5ffb7fb")
    );

    public static final Map<String, String> CHECK = Map.ofEntries(
            Map.entry("zh_cn", "f1eb77633be9e73e04e6f3452d3869fa2b849438b1e0bf738d61452c182c5e26c9d86d259e20a88505ad262d9daf8c1d"),
            Map.entry("en_us", "543f8d6c4c8ba78365cf0fde9f4fe52f18800ac0ad501e743372134884e3bc9f9f6b451d0376b3997d5c2159e6831d3a83d6b3ce6b4d5749cba96b894d2a4d275a882e9ef1e9f912c35ee3288814f9af"),
            Map.entry("ru_ru", "919f19db6ab077582006a5cea87bc4cd2b55261c3fe7f9b2adc664b471a91b32b6f00e69b00f5459284131d9a1e25d6f4bcd008607c0864ca12997775206e23f5db3336efd02723641837bd71275be2db55930d9069738256fe3e510a06d90ef630e93f0cb8c8e6cc78a2820cff634cde5d9b7a6bd745fcb04f5c425c6bb52949307b82fed4e12669316dcd627b2062f"),
            Map.entry("es_es", "7365c7df00b4389f22a7369f9ed9d7e68a877845ca5bd1e5b7d72440b25b9983e7b333614ca296bcfc0241bbdf7d777aa2c0ba801c4a7c2077306a48f2c71b8f2381363924a419eeecf26019be7aa0ff"),
            Map.entry("ja_jp", "7da81fdb4d2827986e8ebcd0f1e0f9701a4953bbb4c235bc9ad871c63dd1c88d1c6a751bd546e86962edbeb4804f982c08b744400f9ffe2311eea086d18f9b781738259998acd35b4478b8c6801a79de390cdc2b43a1925d85758dc42be1e3c1")
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
