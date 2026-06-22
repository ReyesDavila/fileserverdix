package com.reyesdavila.fileserverdix;

import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.ServerSocket;

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

        // Inicializar UI
        setButtonState(false);
        prepararEntorno();

        btnToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isServerRunning) {
                    iniciarServidor();
                } else {
                    detenerServidor();
                }
            }
        });
    }

    // --- 1. PREPARACIÓN DE ARCHIVOS (El puente entre PC y Android) ---
    private void prepararEntorno() {
        try {
            File internalDir = getFilesDir();
            // Truco maestro: Engañar a Java para que crea que su directorio raíz es getFilesDir()
            System.setProperty("user.dir", internalDir.getAbsolutePath());

            // Crear carpetas necesarias
            new File(internalDir, "archivos").mkdirs();
            File staticDir = new File(internalDir, "public_html");
            staticDir.mkdirs();

            // Copiar HTML/JS/CSS desde assets
            String[] webFiles = getAssets().list("public_html");
            if (webFiles != null) {
                for (String file : webFiles) {
                    copyAsset("public_html/" + file, new File(staticDir, file));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error preparando entorno", Toast.LENGTH_LONG).show();
        }
    }

    private void configurarJson(int port) {
        try {
            // Leer config.json original de assets
            InputStream is = getAssets().open("config.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");

            // Reemplazar claves para Android (sin usar librerías externas)
            String dirArchivos = getFilesDir().getAbsolutePath() + "/archivos";
            String dirWeb = getFilesDir().getAbsolutePath() + "/public_html";

            json = json.replaceAll("\"storage_dir\"\\s*:\\s*\".*?\"", "\"storage_dir\": \"" + dirArchivos + "\"");
            json = json.replaceAll("\"static_dir\"\\s*:\\s*\".*?\"", "\"static_dir\": \"" + dirWeb + "\"");
            json = json.replaceAll("\"selected_port\"\\s*:\\s*\\d+", "\"selected_port\": " + port);

            // Guardar el nuevo config.json en el almacenamiento interno para que MonoServer lo lea
            File configFile = new File(getFilesDir(), "config.json");
            FileOutputStream fos = new FileOutputStream(configFile);
            fos.write(json.getBytes("UTF-8"));
            fos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void copyAsset(String assetPath, File destFile) throws Exception {
        InputStream in = getAssets().open(assetPath);
        FileOutputStream out = new FileOutputStream(destFile);
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        in.close();
        out.flush();
        out.close();
    }

    // --- 2. GESTIÓN DEL SERVIDOR ---
    private void iniciarServidor() {
        String portStr = etPort.getText().toString();
        if (portStr.isEmpty()) return;

        final int port = Integer.parseInt(portStr);
        configurarJson(port); // Crea el config.json modificado en tiempo real

        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Forzamos a que MonoServer recargue la configuración estática
                    MonoServer.STORAGE_DIR = getFilesDir().getAbsolutePath() + "/archivos";
                    MonoServer.STATIC_DIR = getFilesDir().getAbsolutePath() + "/public_html";
                    MonoServer.PORT = port;

                    MonoServer.main(new String[]{}); // Lanza el servidor
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        serverThread.start();

        isServerRunning = true;
        setButtonState(true);

        // Mostrar URL
        String ip = MonoServer.getLocalIp();
        tvUrl.setText("http://" + ip + ":" + port);
        tvUrl.setVisibility(View.VISIBLE);
        etPort.setEnabled(false);
    }

    private void detenerServidor() {
        try {
            // Apaga el socket desde aquí
            if (MonoServer.serverSocket != null && !MonoServer.serverSocket.isClosed()) {
                MonoServer.serverSocket.close();
            }
            if (serverThread != null) {
                serverThread.interrupt();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        isServerRunning = false;
        setButtonState(false);
        tvUrl.setVisibility(View.GONE);
        etPort.setEnabled(true);
    }

    // --- 3. DISEÑO NATIVO DEL BOTÓN ---
    @SuppressWarnings("deprecation")
    private void setButtonState(boolean conectado) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);

        if (conectado) {
            shape.setColor(Color.parseColor("#4CAF50")); // Verde F-Droid
            btnToggle.setText("Conectado");
        } else {
            shape.setColor(Color.parseColor("#F44336")); // Rojo
            btnToggle.setText("Desconectado");
        }

        // Magia de retrocompatibilidad extrema
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            // Para Android 4.1 o superior
            btnToggle.setBackground(shape);
        } else {
            // Para tu objetivo de Android 4.0 (API 14)
            btnToggle.setBackgroundDrawable(shape);
        }
    }
}