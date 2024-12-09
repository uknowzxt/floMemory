package com.uknowz.controller;


import com.alibaba.fastjson.JSONObject;
import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;
import org.bouncycastle.asn1.cms.MetaData;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.stream.IntStream;

public class testswing2 {

    String tip1 = "请输入包链接";


    JFrame f = new JFrame("获取包名");

    //定义一个按钮，并为其指定图标
//    Icon okIcon = new ImageIcon("");
    JButton ok = new JButton("go");


    //定义一个40列的单行文本域
    JTextField input = new JTextField(40);





    //初始化界面
    public void init(){

        //------------------------组合主区域------------------------
        //输入框
        input.setText(tip1);//下载链接url
        input.setForeground(Color.GRAY);

        //按钮
        ok.requestFocus();//焦点在按钮上,不然会到默认第一个框中

        //创建一个装载文本框和按钮的JPanel
        JPanel bottom = new JPanel();
        bottom.add(input);
        bottom.add(ok);

        input.addFocusListener(new FocusListener(){

            @Override
            public void focusGained(FocusEvent e) {
                if (input.getText().equals(tip1)){
                    input.setText("");     //将提示文字清空
                    input.setForeground(Color.black);  //设置用户输入的字体颜色为黑色
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (input.getText().equals("")){
                    input.setForeground(Color.gray); //将提示文字设置为灰色
                    input.setText(tip1);     //显示提示文字
                }
            }
        });



        ok.addActionListener(e -> {
            String urlString = input.getText();
            try {

                JSONObject aPackage = getPackage(urlString);

                // 提取 packageName、versionCode 和 versionName
                String packageName = aPackage.getString("packageName");
                String appName = aPackage.getString("appName");
                Long versionCode = aPackage.getLong("versionCode");
                String versionName = aPackage.getString("versionName");
                String UmengChannel = aPackage.getString("UmengChannel");


                // 将这些字段格式化为多行字符串
                String displayText =
                        "<html>" +
                                "Package Name:     " + packageName + "<br>" +
                                "App Name:         " + appName + "<br>" +
                                "Umeng channel:    " + UmengChannel + "<br>" +
                                "Version Code:       " + versionCode + "<br>" +
                                "Version Name:      " + versionName
                                + "</html>";

                new MyDialogDemo(f,displayText); //弹窗

            }catch (Exception ex){
                new MyDialogDemo(f,"异常: " + ex.getMessage());
            }

        });


        //设置
        f.add(bottom, BorderLayout.CENTER);

        // 设置关闭窗口时退出程序
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //设置jFrame最佳大小并可见
        f.pack();
        f.setVisible(true);
        f.setBounds(500,500,600,100);


    }



    //结果 弹窗的窗口
    class MyDialogDemo extends JDialog {
        public MyDialogDemo(JFrame frame,String warn) {
            super(frame,"结果");
            System.setProperty("java.awt.headless", "false");

            this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
            this.setSize(300, 150);
            this.setLocation(550,550);
            this.setVisible(true);

            // 使用 JTextArea
//            JTextArea textArea = new JTextArea(warn);
//            textArea.setEditable(false);  // 设置为只读模式，防止编辑
//            textArea.setLineWrap(true);   // 设置换行
//            textArea.setWrapStyleWord(true); // 设置换行方式
//            textArea.setCaretPosition(0);  // 将光标定位到文本开头，确保用户能看到全部内容

            JLabel label = new JLabel(warn);
            label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));  // 设置鼠标指针为手型

            label.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    // 只复制 PackageName
                    String packageName = extractPackageName(label.getText()); // 提取 packageName
                    if (packageName == null || packageName.isEmpty()){
                        JOptionPane.showMessageDialog(frame, "没有获取到包名!");
                    }
                    StringSelection selection = new StringSelection(packageName);
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipboard.setContents(selection, null);  // 将文本复制到剪贴板
                    JOptionPane.showMessageDialog(frame, "包名已复制到剪贴板!");
                }
            });

//            textArea.setFont(new Font("微软雅黑", Font.PLAIN, 14)); // 为了能够选中并复制文本，设置字体和其他外观
//            JScrollPane scrollPane = new JScrollPane(textArea);  // 添加滚动条，防止文本超出边界

            this.getContentPane().add(label);

            SwingUtilities.invokeLater(() -> { // 确保在 EDT 中执行
                this.getContentPane().add(label);
                this.validate(); // 强制更新布局
                this.repaint();  // 强制重绘窗口
                this.setVisible(true); // 显示窗口
            });
        }
    }

    /**
     * 获取包名
     */
    public static JSONObject getPackage(String urlString) throws IOException, NoSuchAlgorithmException, KeyManagementException {

//        // 设置TLS协议
//        System.setProperty("https.protocols", "TLSv1.2,TLSv1.1,TLSv1");

        // 配置 SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        }, new java.security.SecureRandom());

        // 将配置的 SSLContext 设置为默认 SSLContext
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

        // 设置忽略主机名验证（如果需要）
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);


        // 从URL获取APK文件
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();


        connection.setRequestMethod("GET");
        connection.setConnectTimeout(1000 * 60 * 60); // 设置连接超时
        connection.setReadTimeout(1000 * 60 * 60);    // 设置读取超时

        // 模拟浏览器的请求头
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        connection.setRequestProperty("accept-encoding", "gzip, deflate, br, zstd");
        connection.setRequestProperty("accept-language", "zh-CN,zh;q=0.9");
        connection.setRequestProperty("Connection", "keep-alive");
//        connection.setRequestProperty("Host", "120-244-196-14-ad990f2a.ksyungslb.com:26994");
        connection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");
        connection.setRequestProperty("Upgrade-Insecure-Requests", "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"");
        connection.setRequestProperty("sec-ch-ua", "1");
        connection.setRequestProperty("sec-ch-ua-mobile", "?0");
        connection.setRequestProperty("sec-ch-ua-platform", "Windows");
        connection.setRequestProperty("sec-fetch-dest", "navigate");
        connection.setRequestProperty("sec-fetch-site", " ");
        connection.setRequestProperty("sec-fetch-user", "?1");



        // 检查是否发生了重定向
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
            // 获取重定向的URL
            String newLocation = connection.getHeaderField("Location");
            if (newLocation != null) {
                // 重新请求重定向的URL
                connection = (HttpURLConnection) new URL(newLocation).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(1000 * 60 * 60); // 设置连接超时
                connection.setReadTimeout(1000 * 60 * 60);    // 设置读取超时
                connection.setRequestProperty("accept", "*/*");
                connection.setRequestProperty("connection", "Keep-Alive");
                connection.setRequestProperty("user-agent",
                        "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            }
        }


        // 获取输入流
        try (InputStream inputStream = connection.getInputStream()) {
            // 创建临时文件
            File tempFile = File.createTempFile("temp_apk", ".apk");
            tempFile.deleteOnExit();  // 确保程序退出时删除临时文件

            // 将下载的APK文件保存到临时文件
            try (OutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            JSONObject appInfo = new JSONObject();
            // 使用ApkFile解析包名
            try (ApkFile apkParser = new ApkFile(tempFile)) {
                ApkMeta apkMeta = apkParser.getApkMeta();
                String appName = apkMeta.getLabel();
                String packageName = apkMeta.getPackageName();
                Long versionCode = apkMeta.getVersionCode();
                String versionName = apkMeta.getVersionName();
                // 提取友盟渠道号
                String UmengChannel = getUmengChannel(apkParser);

                appInfo.put("packageName", packageName);
                appInfo.put("versionCode",versionCode);
                appInfo.put("versionName",versionName);
                appInfo.put("UmengChannel",UmengChannel);
                appInfo.put("appName",appName);



                return appInfo;
            }
        }




    }


    // 获取友盟渠道号
    private static String getUmengChannel(ApkFile apkFile) {
        try {
            // 获取 AndroidManifest.xml
            String manifestXml = apkFile.getManifestXml();
            if (manifestXml == null || manifestXml.isEmpty()) {
                return " ";
            }

            // 使用 XML 解析器读取 AndroidManifest.xml 字符串
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(manifestXml.getBytes(StandardCharsets.UTF_8)));

            // 查找 <meta-data> 标签
            NodeList metaDataNodes = document.getElementsByTagName("meta-data");

            /// 使用 Java 8 Stream 查找 UMENG_CHANNEL
            Optional<Node> umengChannelNode = IntStream.range(0, metaDataNodes.getLength())
                    .mapToObj(metaDataNodes::item)
                    .filter(node -> "UMENG_CHANNEL".equals(getAttributeValue(node, "android:name")))
                    .findFirst();

            // 如果找到了，返回渠道号的值，否则返回 " "
            return umengChannelNode
                    .map(node -> getAttributeValue(node, "android:value"))
                    .orElse(" ");

        } catch (Exception e) {
            e.printStackTrace();  // 记录异常（如果有）
        }
        return " ";  // 如果没有找到渠道号，返回默认值
    }


    // 从 HTML 格式的文本中提取包名
    private String extractPackageName(String displayText) {
        // 简单的正则表达式提取 packageName
        String regex = "(?<=Package Name:)[^<]+";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(displayText);
        if (matcher.find()) {
            return matcher.group(0).trim();  // 返回匹配到的包名
        }
        return ""; // 如果没有匹配到，返回默认值
    }

    // 获取节点的属性值，避免空指针异常
    private static String getAttributeValue(Node node, String attributeName) {
        Node attribute = node.getAttributes().getNamedItem(attributeName);
        return attribute != null ? attribute.getNodeValue() : null;
    }

    public static void main(String[] args) {
        new testswing2().init();
    }



}


