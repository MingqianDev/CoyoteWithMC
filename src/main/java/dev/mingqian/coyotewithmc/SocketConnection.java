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
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
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

    private static void genQRCode() throws WriterException, IOException {
        String content = "";
        final int width = 400;
        final int height = 400;
        final String charset = "UTF-8";
        final String imgPath = "./resources/assets/coyotesocketcontrol/images/DG-LAB logo.jpg";
        final String destPath = "QRCode.png";

        if (!clientId.isEmpty()) {
            content = "https://www.dungeon-lab.com/app-download.php#DGLAB-SOCKET#" + address + clientId;
        } else {
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("failed to get ClientID please try again"));
        }

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height);

        Path path = FileSystems.getDefault().getPath(destPath);
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);

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
                            Style style = Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, link + address + clientId)).withUnderlined(true);

                            Minecraft.getInstance().player.sendSystemMessage(Component.literal("click me").setStyle(style));

                            //TODO: make QR code


//                            try {
//                                genQRCode();
//                            } catch (WriterException | IOException e) {
//                                e.printStackTrace();
//                            }

                        } else if (!res.get("targetId").getAsString().isEmpty()) {
                            //TODO: received targetId
                            targetId = res.get("targetId").getAsString();
                            System.out.println("got targetId: " + targetId);
                            Minecraft.getInstance().player.sendSystemMessage(Component.literal("got targetId: " + targetId));
                        }
                        break;
                    case "heartbeat":
                        System.out.println("heartbeat received");
                        Minecraft.getInstance().player.sendSystemMessage(Component.literal("heartbeat received"));
                        webSocket.sendText("heartbeat", true);
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

}
