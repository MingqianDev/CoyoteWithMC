package dev.mingqian.coyotewithmc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import dev.mingqian.coyotewithmc.event.ClientEvents;
import dev.mingqian.coyotewithmc.util.QRCodeUtil;
import io.nayuki.qrcodegen.QrCode;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;


public class SocketConnection {
    private static WebSocket webSocket;


    public static String address = "wss://ws.dungeon-lab.cn/";
    public static String clientId = "";
    public static String targetId = "";
    public static String[] wave = {"0A0A0A0A00000000","0A0A0A0A0A0A0A0A","0A0A0A0A14141414","0A0A0A0A1E1E1E1E","0A0A0A0A28282828","0A0A0A0A32323232","0A0A0A0A3C3C3C3C","0A0A0A0A46464646","0A0A0A0A50505050","0A0A0A0A5A5A5A5A","0A0A0A0A64646464"};

    public static void sendPulse() {
        JsonObject msg = new JsonObject();
        JsonArray pulseArray = new JsonArray();


        // 发送波形, 5s AB通道一样
        for (String e : wave) {
            pulseArray.add(e);
        }

        msg.addProperty("type", "clientMsg");
        msg.addProperty("message", "A:" + pulseArray.toString());
        msg.addProperty("message2", "B:" + pulseArray.toString());
        msg.addProperty("time1", 1);
        msg.addProperty("time2", 1);
        msg.addProperty("clientId", clientId);
        msg.addProperty("targetId", targetId);


        // 通道A强度 + 1
//        msg.addProperty("type", 2);
//        msg.addProperty("strength", 1);
//        msg.addProperty("message", "set channel");
//        msg.addProperty("clientId", clientId);
//        msg.addProperty("targetId", targetId);


        webSocket.sendText(msg.toString(), true);
        System.out.println(msg.toString());
    }

    private static void genQRCode() throws IOException {
        String content = "";
        final int width = 400;
        final int height = 400;
        final String charset = "UTF-8";
        final String destPath = "QRCode.png";
        // Error correction level
        QrCode.Ecc errCorLvl = QrCode.Ecc.MEDIUM;
        File file = new File("QRCode.png");
//        file.mkdir();

        if (!clientId.isEmpty()) {
            content = "https://www.dungeon-lab.com/app-download.php#DGLAB-SOCKET#" + address + clientId;
        } else {
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("failed to get ClientID please try again"));
            return;
        }

        Style style = Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file.getAbsolutePath())).withUnderlined(true);
        Minecraft.getInstance().player.sendSystemMessage(Component.literal("Click here to open QrCode").setStyle(style));

        // generate Qr code
        QrCode qr = QrCode.encodeText(content, errCorLvl);  // Make the QR Code symbol

        BufferedImage img = toImage(qr, 10, 4);
        ImageIO.write(img, "png", file);
        QRCodeWriter qrCodeWriter = new QRCodeWriter();





//        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height);
//
//        Path path = FileSystems.getDefault().getPath(destPath);
//        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);

    }


    public static void connectServer() {
        HttpClient client = HttpClient.newHttpClient();
        WebSocket.Listener listener = new WebSocket.Listener(){
            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                // 把json转换成Gson支持的格式
                JsonObject res = new JsonParser().parse(data.toString()).getAsJsonObject();
                System.out.println("got message " + data.toString());

                switch (res.get("type").getAsString()) {
                    case "bind" :
                        if (res.get("message").getAsString().equals("targetId")) {
                            // 第一次连接 获取clientId
                            clientId = res.get("clientId").getAsString();

                            System.out.println("clientId received " + clientId);
//                            Minecraft.getInstance().player.sendSystemMessage(Component.literal("clientId received " + clientId));

                            String link = "https://www.dungeon-lab.com/app-download.php#DGLAB-SOCKET#" + address + clientId;


                            //TODO: make QR code


                            try {
                                genQRCode();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        } else if (!res.get("targetId").getAsString().isEmpty()) {
                            //TODO: received targetId
                            targetId = res.get("targetId").getAsString();
                            System.out.println("got targetId: " + targetId);
                            Minecraft.getInstance().player.sendSystemMessage(Component.literal("Successfully connected to the DG-LAB app!"));
                        }
                        break;
                    case "heartbeat":
                        System.out.println("heartbeat received");
                        break;
                    case "break":
                        //TODO
                        break;
                    case "error" :
                        //TODO
                        break;
                    case "msg" :
                        //TODO
                        break;
                }
                webSocket.request(1);
                return null; // 返回 null 表示不需要后续的处理
            }
        };

        // 连接websocket服务器

        try {
            webSocket = client.newWebSocketBuilder()
                    .buildAsync(URI.create(address), listener)
                    .join();

        } catch (Exception e) {
            // 处理其他可能的异常
            e.printStackTrace();
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("Connection fail, please check Websocket server URL"));
        }

    }

    /*---- Utilities ----*/

    private static BufferedImage toImage(QrCode qr, int scale, int border) {
        return toImage(qr, scale, border, 0xFFFFFF, 0x000000);
    }

    private static BufferedImage toImage(QrCode qr, int scale, int border, int lightColor, int darkColor) {
        Objects.requireNonNull(qr);
        if (scale <= 0 || border < 0)
            throw new IllegalArgumentException("Value out of range");
        if (border > Integer.MAX_VALUE / 2 || qr.size + border * 2L > Integer.MAX_VALUE / scale)
            throw new IllegalArgumentException("Scale or border too large");

        BufferedImage result = new BufferedImage((qr.size + border * 2) * scale, (qr.size + border * 2) * scale, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < result.getHeight(); y++) {
            for (int x = 0; x < result.getWidth(); x++) {
                boolean color = qr.getModule(x / scale - border, y / scale - border);
                result.setRGB(x, y, color ? darkColor : lightColor);
            }
        }
        return result;
    }

}
