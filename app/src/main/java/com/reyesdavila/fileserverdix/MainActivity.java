package com.reyesdavila.fileserverdix;
import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private EditText etPort;
    private Button btnToggle;
    private TextView tvUrl;
    private boolean isServerRunning = false;
    private Thread serverThread;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        etPort = findViewById(R.id.etPort);
        btnToggle = findViewById(R.id.btnToggle);
        tvUrl = findViewById(R.id.tvUrl);
        setButtonState(false);
        prepararEntorno();
        btnToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isServerRunning) iniciarServidor();
                else detenerServidor();
            }
        });
    }
    // --- 1. PREPARACIÓN DE ENTORNO Y RUTAS ---
    private void prepararEntorno() {
        try {
            File internalDir = getFilesDir();
            System.setProperty("user.dir", internalDir.getAbsolutePath());
            // Crear carpeta pública para archivos (Descargas) y privada para la web
            File dirArchivos = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (dirArchivos != null && !dirArchivos.exists()) dirArchivos.mkdirs();
            File staticDir = new File(internalDir, "public_html");
            staticDir.mkdirs();
            // Extraer web desde assets
            String[] webFiles = getAssets().list("public_html");
            if (webFiles != null) {
                for (String file : webFiles) copyAsset("public_html/" + file, new File(staticDir, file));
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error inicializando entorno", Toast.LENGTH_SHORT).show();
        }
    }
    private void copyAsset(String assetPath, File destFile) throws Exception {
        InputStream in = getAssets().open(assetPath);
        FileOutputStream out = new FileOutputStream(destFile);
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
        in.close();
        out.flush();
        out.close();
    }
    private void configurarJson(int port) {
        try {
            InputStream is = getAssets().open("config.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");
            // Se inyecta ruta visible (Descargas) para storage y ruta privada para static_dir
            String dirArchivos = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
            String dirWeb = getFilesDir().getAbsolutePath() + "/public_html";
            json = json.replaceAll("\"storage_dir\"\\s*:\\s*\".*?\"", "\"storage_dir\": \"" + dirArchivos + "\"");
            json = json.replaceAll("\"static_dir\"\\s*:\\s*\".*?\"", "\"static_dir\": \"" + dirWeb + "\"");
            json = json.replaceAll("\"selected_port\"\\s*:\\s*\\d+", "\"selected_port\": " + port);
            FileOutputStream fos = new FileOutputStream(new File(getFilesDir(), "config.json"));
            fos.write(json.getBytes("UTF-8"));
            fos.close();
        } catch (Exception e) { e.printStackTrace(); }
    }
    // --- 2. GESTIÓN DEL SERVIDOR ---
    private void iniciarServidor() {
        String portStr = etPort.getText().toString();
        if (portStr.isEmpty()) return;
        final int port = Integer.parseInt(portStr);
        configurarJson(port);
        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    MonoServer.STORAGE_DIR = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
                    MonoServer.STATIC_DIR = getFilesDir().getAbsolutePath() + "/public_html";
                    MonoServer.PORT = port;
                    MonoServer.main(new String[]{});
                } catch (Exception e) { e.printStackTrace(); }
            }
        });
        serverThread.start();
        isServerRunning = true;
        setButtonState(true);
        tvUrl.setText("http://" + MonoServer.getLocalIp() + ":" + port);
        tvUrl.setVisibility(View.VISIBLE);
        etPort.setEnabled(false);
    }
    private void detenerServidor() {
        try {
            if (MonoServer.serverSocket != null && !MonoServer.serverSocket.isClosed()) MonoServer.serverSocket.close();
            if (serverThread != null) serverThread.interrupt();
        } catch (Exception e) { e.printStackTrace(); }
        isServerRunning = false;
        setButtonState(false);
        tvUrl.setVisibility(View.GONE);
        etPort.setEnabled(true);
    }
    // --- 3. UI Y DISEÑO ---
    @SuppressWarnings("deprecation")
    private void setButtonState(boolean conectado) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setStroke(6, conectado ? Color.parseColor("#39FF14") : Color.parseColor("#8B0000")); // Borde (Neón verde o Rojo oscuro)
        shape.setColor(conectado ? Color.parseColor("#1B301B") : Color.parseColor("#301B1B")); // Fondo oscuro

        if (android.os.Build.VERSION.SDK_INT >= 16) {
            btnToggle.setBackground(shape);
        } else {
            btnToggle.setBackgroundDrawable(shape);
        }

        btnToggle.setText(conectado ? "ON" : "OFF");
    }
}