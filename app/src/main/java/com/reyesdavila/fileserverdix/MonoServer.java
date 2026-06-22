package com.reyesdavila.fileserverdix;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;

public class MonoServer {
    public static int PORT;

    // 1. Aquí se crea el "Pool" (grupo) de 10 hilos prefabricados
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);

    public static String getLocalIp() { try { Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces(); while (interfaces.hasMoreElements()) { NetworkInterface ni = interfaces.nextElement(); if (ni.isLoopback() || !ni.isUp()) continue; Enumeration<InetAddress> addresses = ni.getInetAddresses(); while (addresses.hasMoreElements()) { InetAddress addr = addresses.nextElement(); if (addr instanceof Inet4Address) return addr.getHostAddress(); } } } catch (Exception e) {} return "127.0.0.1"; }
    public static String STORAGE_DIR;
    public static String STATIC_DIR;

    static { try { File f = new File("config.json"); FileInputStream fis = new FileInputStream(f); byte[] data = new byte[(int) f.length()]; fis.read(data); fis.close(); String json = new String(data, "UTF-8"); STORAGE_DIR = Orquestador.extractStr(json, "storage_dir"); STATIC_DIR = Orquestador.extractStr(json, "static_dir"); String portStr = Orquestador.extractStr(json, "selected_port"); PORT = (portStr != null) ? Integer.parseInt(portStr) : 8080; } catch (Exception e) { e.printStackTrace(); } }

    public static Orquestador orc;

    // 1. Añade esta variable justo encima del main
    public static ServerSocket serverSocket;

    public static void main(String[] args) throws Exception {
        String ip = getLocalIp();
        orc = new Orquestador(STORAGE_DIR, ip, PORT);

        // 2. Usar la variable estática aquí
        serverSocket = new ServerSocket(PORT);
        System.out.println("--- Servidor Java activo en http://" + ip + ":" + PORT + " ---");

        while(true) {
            try {
                final Socket client = serverSocket.accept();
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        handleClient(client);
                    }
                });
            } catch (SocketException e) {
                // Si el socket se cierra desde MainActivity, rompe el bucle para detener el servidor limpiamente
                System.out.println("Servidor apagado.");
                break;
            }
        }
    }

    private static void handleClient(Socket s) { try { InputStream in = s.getInputStream(); OutputStream out = s.getOutputStream(); BufferedReader br = new BufferedReader(new InputStreamReader(in)); String line = br.readLine(); if(line==null) return; String[] req = line.split(" "); int contentLen = 0; while(!(line = br.readLine()).isEmpty()) { if(line.toLowerCase().startsWith("content-length:")) contentLen = Integer.parseInt(line.split(":")[1].trim()); } if(req[0].equals("POST") && req[1].equals("/api")) { char[] bodyChars = new char[contentLen]; int read = 0; while(read < contentLen) { int r = br.read(bodyChars, read, contentLen - read); if(r == -1) break; read += r; } String body = new String(bodyChars); String action = Orquestador.extractStr(body, "action"); String response = orc.procesar(action, body); sendHeader(out, 200, "application/json", response.getBytes("UTF-8").length, null, null); out.write(response.getBytes("UTF-8")); out.flush(); } else if(req[0].equals("GET")) { handleGet(out, req[1]); } s.close(); } catch(Exception e) { e.printStackTrace(); } }
    private static void handleGet(OutputStream out, String path) { try { if(path.startsWith("/download/") || path.startsWith("/view/")) { String fname = URLDecoder.decode(path.substring(path.lastIndexOf("/")+1), "UTF-8"); File f = new File(orc.dir, fname); if(f.exists() && !f.isDirectory()) { byte[] data = readFileBytes(f); sendHeader(out, 200, guessMime(fname), data.length, path.startsWith("/download") ? "attachment" : "inline", fname); out.write(data); out.flush(); } else { sendHeader(out, 404, "text/plain", 9, null, null); out.write("Not Found".getBytes()); } } else { String fname = path.equals("/") ? "index.html" : path.substring(1); File f = new File(STATIC_DIR, fname); if(f.exists() && !f.isDirectory()) { byte[] data = readFileBytes(f); sendHeader(out, 200, guessMime(fname), data.length, null, null); out.write(data); out.flush(); } else { sendHeader(out, 404, "text/plain", 9, null, null); out.write("Not Found".getBytes()); } } } catch(Exception e) { e.printStackTrace(); } }
    private static byte[] readFileBytes(File f) throws Exception { FileInputStream fis = new FileInputStream(f); ByteArrayOutputStream bos = new ByteArrayOutputStream(); byte[] buf = new byte[8192]; int len; while((len=fis.read(buf))!=-1) bos.write(buf, 0, len); fis.close(); return bos.toByteArray(); }
    private static void sendHeader(OutputStream os, int code, String cType, int len, String disp, String fname) throws Exception { String h = "HTTP/1.1 " + code + " OK\r\nAccess-Control-Allow-Origin: *\r\nContent-Type: " + cType + "\r\nContent-Length: " + len + "\r\n"; if(disp!=null) h += "Content-Disposition: " + disp + "; filename=\"" + fname + "\"\r\n"; os.write((h + "\r\n").getBytes("UTF-8")); }
    public static String guessMime(String fname) { String f = fname.toLowerCase(); if(f.endsWith(".html")) return "text/html"; if(f.endsWith(".js")) return "application/javascript"; if(f.endsWith(".css")) return "text/css"; if(f.endsWith(".json")) return "application/json"; if(f.endsWith(".png")) return "image/png"; if(f.endsWith(".jpg") || f.endsWith(".jpeg")) return "image/jpeg"; if(f.endsWith(".gif")) return "image/gif"; if(f.endsWith(".pdf")) return "application/pdf"; if(f.endsWith(".mp4")) return "video/mp4"; if(f.endsWith(".zip")) return "application/zip"; if(f.endsWith(".ico")) return "image/x-icon"; return "application/octet-stream"; }
}

class Orquestador {
    public String dir;
    public String host;
    public int port;
    public ConcurrentHashMap<String, String> tickets = new ConcurrentHashMap<>();
    public Orquestador(String storage_dir, String host, int port) { this.dir = storage_dir; this.host = host; this.port = port; File d = new File(this.dir); if(!d.exists()) d.mkdirs(); }
    public String procesar(String action, String body) { if(action == null) return "{\"status\": \"error\"}"; switch(action) { case "fetch": return _action_fetch(body); case "upload": return _action_upload(body); case "download": return _action_download(body); case "view": return _action_view(body); case "delete": return _action_delete(body); case "delete_all": return _action_delete_all(body); case "download_all": return _action_download_all(body); case "check_status": return _check_status(body); default: return "{\"status\": \"error\"}"; } }

    private String _check_status(String body) {
        String val = tickets.get(extractStr(body, "ticket"));
        return (val != null) ? val : "{\"ready\": false}";
    }

    private String _action_fetch(String body) { try { File[] files = new File(this.dir).listFiles(); StringBuilder sb = new StringBuilder("["); SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); boolean first = true; if(files != null) { for(File f : files) { if(!f.getName().startsWith("temp_zip_")) { if(!first) sb.append(","); sb.append("{\"name\": \"").append(f.getName()).append("\", \"date\": \"").append(sdf.format(f.lastModified())).append("\"}"); first = false; } } } sb.append("]"); return "{\"files\": " + sb.toString() + ", \"ip\": \"" + this.host + "\", \"port\": " + this.port + "}"; } catch(Exception e) { return "{\"status\": \"error\"}"; } }
    private String _action_upload(String body) { try { FileOutputStream fos = new FileOutputStream(new File(this.dir, extractStr(body, "filename"))); fos.write(decodeBase64(extractStr(body, "data"))); fos.close(); return "{\"status\": \"ok\"}"; } catch(Exception e) { return "{\"status\": \"error\"}"; } }
    private String _action_download(String body) { String tid = UUID.randomUUID().toString(); tickets.put(tid, "{\"ready\": true, \"url\": \"/download/" + extractStr(body, "filename") + "\"}"); return "{\"status\": \"ok\", \"ticket\": \"" + tid + "\"}"; }

    private String _action_download_all(String body) {
        final String tid = UUID.randomUUID().toString();
        tickets.put(tid, "{\"ready\": false}");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File d = new File(dir);
                    File[] files = d.listFiles();
                    if(files != null) {
                        for(File f : files) {
                            if(f.getName().startsWith("temp_zip_")) f.delete();
                        }
                    }
                    String zName = "temp_zip_" + tid + ".zip";
                    FileOutputStream fos = new FileOutputStream(new File(d, zName));
                    ZipOutputStream zos = new ZipOutputStream(fos);
                    if(files != null) {
                        for(File f : files) {
                            if(!f.getName().startsWith("temp_zip_") && !f.getName().equals(zName)) {
                                zos.putNextEntry(new ZipEntry(f.getName()));
                                FileInputStream fis = new FileInputStream(f);
                                byte[] buf = new byte[8192];
                                int len;
                                while((len = fis.read(buf)) > 0) zos.write(buf, 0, len);
                                fis.close();
                                zos.closeEntry();
                            }
                        }
                    }
                    zos.close();
                    fos.close();
                    tickets.put(tid, "{\"ready\": true, \"url\": \"/download/" + zName + "\"}");
                } catch(Exception e) {}
            }
        }).start();

        return "{\"status\": \"ok\", \"ticket\": \"" + tid + "\"}";
    }

    private String _action_view(String body) { return "{\"status\": \"ok\", \"url\": \"/view/" + extractStr(body, "filename") + "\"}"; }
    private String _action_delete(String body) { try { new File(this.dir, extractStr(body, "filename")).delete(); return "{\"status\": \"ok\"}"; } catch(Exception e) { return "{\"status\": \"error\"}"; } }
    private String _action_delete_all(String body) { try { File[] files = new File(this.dir).listFiles(); if(files != null) for(File f : files) if(!f.getName().startsWith("temp_zip_")) f.delete(); return "{\"status\": \"ok\"}"; } catch(Exception e) { return "{\"status\": \"error\"}"; } }
    private byte[] decodeBase64(String s) throws Exception { try { Object decoder = Class.forName("java.util.Base64").getMethod("getDecoder").invoke(null); return (byte[]) decoder.getClass().getMethod("decode", String.class).invoke(decoder, s); } catch(Exception e) { return (byte[]) Class.forName("android.util.Base64").getMethod("decode", String.class, int.class).invoke(null, s, 0); } }
    public static String extractStr(String json, String key) { try { String search = "\"" + key + "\""; int start = json.indexOf(search); if(start == -1) return null; start = json.indexOf(":", start) + 1; while(start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\n' || json.charAt(start) == '\r')) start++; if(json.charAt(start) == '"') { start++; return json.substring(start, json.indexOf("\"", start)); } else { int end = start; while(end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++; return json.substring(start, end).trim(); } } catch(Exception e) { return null; } }
}