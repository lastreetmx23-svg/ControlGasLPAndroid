package com.controlgaslp.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.content.res.ColorStateList;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Native Android interface for Control Gas LP. Uses a dedicated, key-protected Apps Script API. */
public class MainActivity extends Activity {
    private static final int NAVY = Color.rgb(16, 42, 67);
    private static final int GREEN = Color.rgb(26, 143, 92);
    private static final int BG = Color.rgb(245, 247, 250);
    private static final int TEXT = Color.rgb(28, 39, 49);
    private static final int MUTED = Color.rgb(95, 108, 120);
    private static final String PREFS = "control_gas_lp_settings";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private SharedPreferences preferences;
    private String apiUrl = "";
    private String apiKey = "";
    private JSONObject appData;
    private LinearLayout root;
    private LinearLayout body;
    private String activeScreen = "home";
    private String settingsMessage = "";

    interface ApiCallback { void done(JSONObject response, String error); }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        preferences = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        apiUrl = preferences.getString("url", "");
        apiKey = preferences.getString("key", "");
        if (apiUrl.isEmpty() || apiKey.isEmpty()) showSettings(); else refreshData("home");
    }

    @Override protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }

    private void buildShell(String title, boolean showHome) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(16), dp(10), dp(16), dp(12));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(0, dp(8), 0, dp(12));
        TextView heading = new TextView(this);
        heading.setText(title);
        heading.setTextColor(NAVY);
        heading.setTextSize(22);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        bar.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        if (showHome) {
            Button home = smallButton("Inicio");
            home.setOnClickListener(v -> showHome());
            bar.addView(home);
        }
        Button settings = smallButton("⚙");
        settings.setContentDescription("Configuración");
        settings.setOnClickListener(v -> showSettings());
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(44), dp(42));
        sp.leftMargin = dp(6);
        bar.addView(settings, sp);
        root.addView(bar);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(0, dp(2), 0, dp(12));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView footer = new TextView(this);
        footer.setText("CONTROL GAS LP  •  Datos sincronizados en la nube");
        footer.setTextColor(MUTED);
        footer.setTextSize(11);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(8), 0, 0);
        root.addView(footer);
        setContentView(root);
    }

    private void showHome() {
        activeScreen = "home";
        buildShell("Control Gas LP", false);
        addText(body, "Suministro en pipa · Panel de control", 14, MUTED, false);
        addSpace(body, 10);
        JSONObject dash = appData == null ? new JSONObject() : appData.optJSONObject("dashboard");
        if (dash == null) dash = new JSONObject();
        addMetric("Ventas de hoy", money(dash.optDouble("ventasHoy", 0)), "Entregas: " + dash.optInt("entregasHoy", 0));
        addMetric("Ventas del mes", money(dash.optDouble("ventasMes", 0)), "Litros: " + qty(dash.optDouble("litrosMes", 0)));
        addMetric("Cobros del mes", money(dash.optDouble("cobrosMes", 0)), "Pagos registrados");
        addMetric("Saldo por cobrar", money(dash.optDouble("saldoPendiente", 0)), "Cuentas pendientes");
        addMetric("Gastos del mes", money(dash.optDouble("gastosMes", 0)), "Flujo registrado: " + money(dash.optDouble("flujoMes", 0)));
        addMetric("Clientes activos", String.valueOf(dash.optInt("clientesActivos", 0)), "Eventos pendientes: " + dash.optInt("agendaPendiente", 0));
        addSpace(body, 8);
        addText(body, "Acciones rápidas", 18, NAVY, true);
        addAction("＋  Nueva venta", () -> showSaleForm());
        addAction("＋  Registrar cliente", () -> showClientForm());
        addAction("$  Registrar abono", () -> showCredits());
        addAction("▦  Clientes", () -> showClients());
        addAction("▣  Agenda", () -> showAgenda());
        addAction("−  Gastos", () -> showExpenses());
        addAction("▤  Reportes", () -> showReports());
        addSpace(body, 6);
        Button refresh = secondaryButton("Actualizar datos");
        refresh.setOnClickListener(v -> refreshData("home"));
        body.addView(refresh, matchWrap());
    }

    private void showSettings() {
        activeScreen = "settings";
        buildShell("Conectar aplicación", !((apiUrl.isEmpty() || apiKey.isEmpty()) && appData == null));
        addText(body, "Conecta esta app Android con la API privada de tu hoja Control Gas LP.", 15, TEXT, false);
        addSpace(body, 10);
        addText(body, "URL de la API (termina en /exec)", 13, NAVY, true);
        EditText urlField = field("https://script.google.com/macros/s/…/exec", apiUrl, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        body.addView(urlField, matchWrap());
        addSpace(body, 8);
        addText(body, "Clave API", 13, NAVY, true);
        EditText keyField = field("Pega aquí API_KEY", apiKey, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        body.addView(keyField, matchWrap());
        addSpace(body, 8);
        addText(body, "No uses aquí la URL de la aplicación web actual. Debe ser la URL de una implementación API separada, según la guía incluida en el paquete.", 13, MUTED, false);
        if (!settingsMessage.isEmpty()) {
            addSpace(body, 8);
            addText(body, settingsMessage, 13, NAVY, false);
        }
        addSpace(body, 14);
        Button connect = primaryButton("Probar conexión y guardar");
        connect.setOnClickListener(v -> {
            String newUrl = urlField.getText().toString().trim();
            String newKey = keyField.getText().toString().trim();
            if (!newUrl.startsWith("https://script.google.com/macros/s/") || !newUrl.endsWith("/exec")) {
                toast("La URL debe comenzar con https://script.google.com/macros/s/ y terminar en /exec.");
                return;
            }
            if (newKey.length() < 20) { toast("Revisa que hayas pegado la clave API completa."); return; }
            apiUrl = newUrl; apiKey = newKey;
            preferences.edit().putString("url", apiUrl).putString("key", apiKey).apply();
            JSONObject request = new JSONObject();
            try { request.put("action", "ping"); } catch (JSONException ignored) {}
            connect.setEnabled(false); connect.setText("Conectando…");
            apiRequest(request, (response, error) -> {
                connect.setEnabled(true); connect.setText("Probar conexión y guardar");
                if (error == null && response.optBoolean("ok")) {
                    settingsMessage = "Conexión correcta. Ya puedes usar la aplicación.";
                    toast("Conexión correcta");
                    refreshData("home");
                } else {
                    settingsMessage = error == null ? response.optString("error", "No se pudo validar la API.") : error;
                    addText(body, settingsMessage, 13, Color.RED, false);
                }
            });
        });
        body.addView(connect, matchWrap());
        addSpace(body, 12);
        Button cancel = secondaryButton("Volver al panel");
        cancel.setOnClickListener(v -> { if (appData != null) showHome(); else toast("Configura la API para entrar."); });
        body.addView(cancel, matchWrap());
    }

    private void showClients() {
        activeScreen = "clients";
        buildShell("Clientes", true);
        Button add = primaryButton("＋ Nuevo cliente"); add.setOnClickListener(v -> showClientForm()); body.addView(add, matchWrap());
        JSONArray clients = array("clients");
        if (clients.length() == 0) addText(body, "Todavía no hay clientes.", 14, MUTED, false);
        for (int i = 0; i < clients.length(); i++) {
            JSONObject c = clients.optJSONObject(i); if (c == null) continue;
            String title = c.optString("Nombre", "Sin nombre");
            String detail = c.optString("Tipo cliente", "") + "\n" + c.optString("Teléfono", "") + "\n" + c.optString("Dirección", "");
            addInfoCard(title, detail.trim());
        }
    }

    private void showClientForm() {
        activeScreen = "clientForm";
        buildShell("Nuevo cliente", true);
        EditText name = addField("Nombre del cliente *", "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        Spinner type = addSpinner(new String[]{"Hogar", "Comercio", "Empresa"});
        EditText phone = addField("Teléfono", "", InputType.TYPE_CLASS_PHONE);
        EditText address = addField("Dirección", "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        EditText ref = addField("Referencia para llegar", "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        EditText notes = addField("Notas", "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        Button save = primaryButton("Guardar cliente");
        save.setOnClickListener(v -> {
            if (name.getText().toString().trim().isEmpty()) { toast("Escribe el nombre del cliente."); return; }
            JSONObject r = request("saveClient"); put(r,"nombre",name.getText().toString()); put(r,"tipo",type.getSelectedItem().toString()); put(r,"telefono",phone.getText().toString()); put(r,"direccion",address.getText().toString()); put(r,"referencia",ref.getText().toString()); put(r,"notas",notes.getText().toString());
            save.setEnabled(false); apiRequest(r, (res,err) -> { save.setEnabled(true); if (success(res,err)) { toast("Cliente guardado"); refreshData("clients"); } });
        });
        body.addView(save, matchWrap());
    }

    private void showSaleForm() {
        activeScreen = "saleForm";
        buildShell("Nueva venta", true);
        JSONArray clients = array("clients");
        if (clients.length() == 0) {
            addText(body, "Primero registra al menos un cliente.", 15, MUTED, false);
            Button c = primaryButton("Registrar cliente"); c.setOnClickListener(v -> showClientForm()); body.addView(c, matchWrap()); return;
        }
        ArrayList<String> labels = new ArrayList<>(); ArrayList<String> ids = new ArrayList<>();
        for (int i=0;i<clients.length();i++){ JSONObject c=clients.optJSONObject(i); if(c!=null){ labels.add(c.optString("Nombre") + (c.optString("Teléfono").isEmpty()?"":" · "+c.optString("Teléfono"))); ids.add(c.optString("ID Cliente")); } }
        addText(body, "Cliente *", 13, NAVY, true); Spinner client = addSpinner(labels.toArray(new String[0]));
        EditText liters = addField("Litros suministrados *", "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText price = addField("Precio por litro ($) *", "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText initial = addField("Pago recibido ahora ($)", "0", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        addText(body, "Método del pago inicial", 13, NAVY, true); Spinner method = addSpinner(new String[]{"Efectivo", "Transferencia", "Tarjeta", "Otro"});
        EditText notes = addField("Notas de la entrega", "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        addText(body, "El total se calcula automáticamente: litros × precio por litro.", 12, MUTED, false);
        Button save = primaryButton("Guardar venta");
        save.setOnClickListener(v -> {
            if (ids.isEmpty()) { toast("Registra primero un cliente."); return; }
            if (liters.getText().toString().trim().isEmpty() || price.getText().toString().trim().isEmpty()) { toast("Completa litros y precio por litro."); return; }
            JSONObject r=request("saveSale"); put(r,"clienteId",ids.get(Math.max(0,client.getSelectedItemPosition()))); put(r,"litros",liters.getText().toString()); put(r,"precio",price.getText().toString()); put(r,"pagoInicial",initial.getText().toString().isEmpty()?"0":initial.getText().toString()); put(r,"metodoPago",method.getSelectedItem().toString()); put(r,"notas",notes.getText().toString()); put(r,"fechaHora","");
            save.setEnabled(false); apiRequest(r,(res,err)->{save.setEnabled(true);if(success(res,err)){toast("Venta guardada · Total "+money(res.optDouble("total",0)));refreshData("home");}});
        });
        body.addView(save, matchWrap());
    }

    private void showCredits() {
        activeScreen = "credits";
        buildShell("Créditos y cobranza", true);
        JSONArray pending = array("pendingSales");
        if (pending.length()==0) addText(body,"No hay saldos pendientes por cobrar.",14,MUTED,false);
        for (int i=0;i<pending.length();i++) {
            JSONObject sale=pending.optJSONObject(i); if(sale==null)continue;
            LinearLayout card=cardLayout();
            addText(card,sale.optString("Cliente","Cliente"),17,NAVY,true);
            addText(card,"Venta: "+sale.optString("ID Venta")+"\nFecha: "+sale.optString("Fecha y hora")+"\nTotal: "+money(sale.optDouble("Importe total",0))+" · Abonado: "+money(sale.optDouble("Abonado",0)),13,MUTED,false);
            addText(card,"Saldo pendiente: "+money(sale.optDouble("Saldo",0)),16,GREEN,true);
            EditText amount=field("Importe del abono ($)","",InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL); card.addView(amount,matchWrap());
            Spinner method=addSpinnerTo(card,new String[]{"Efectivo","Transferencia","Tarjeta","Otro"});
            Button pay=primaryButton("Registrar abono");
            String id=sale.optString("ID Venta");
            pay.setOnClickListener(v->{if(amount.getText().toString().trim().isEmpty()){toast("Escribe el importe del abono.");return;} JSONObject r=request("recordPayment");put(r,"ventaId",id);put(r,"importe",amount.getText().toString());put(r,"metodoPago",method.getSelectedItem().toString());put(r,"fechaHora","");put(r,"notas","");pay.setEnabled(false);apiRequest(r,(res,err)->{pay.setEnabled(true);if(success(res,err)){toast("Abono guardado · Saldo "+money(res.optDouble("saldoRestante",0)));refreshData("credits");}});});
            card.addView(pay,matchWrap()); body.addView(card,matchWrap());
        }
    }

    private void showAgenda() {
        activeScreen="agenda"; buildShell("Agenda",true);
        addText(body,"Programar suministro, cobranza o seguimiento",14,MUTED,false);
        JSONArray clients=array("clients"); ArrayList<String> labels=new ArrayList<>(); ArrayList<String> ids=new ArrayList<>(); labels.add("Sin cliente");ids.add("");
        for(int i=0;i<clients.length();i++){JSONObject c=clients.optJSONObject(i);if(c!=null){labels.add(c.optString("Nombre"));ids.add(c.optString("ID Cliente"));}}
        addText(body,"Cliente (opcional)",13,NAVY,true); Spinner client=addSpinner(labels.toArray(new String[0]));
        EditText dateTime=addField("Fecha y hora (AAAA-MM-DD HH:MM)",nowDateTime(),InputType.TYPE_CLASS_TEXT);
        Spinner type=addSpinner(new String[]{"Suministro","Cobranza","Seguimiento"});
        EditText notes=addField("Notas", "", InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        Button save=primaryButton("Guardar en agenda");
        save.setOnClickListener(v->{JSONObject r=request("saveAgenda");put(r,"clienteId",ids.get(Math.max(0,client.getSelectedItemPosition())));put(r,"fechaHora",dateTime.getText().toString());put(r,"tipo",type.getSelectedItem().toString());put(r,"notas",notes.getText().toString());save.setEnabled(false);apiRequest(r,(res,err)->{save.setEnabled(true);if(success(res,err)){toast("Evento guardado");refreshData("agenda");}});}); body.addView(save,matchWrap());
        addSpace(body,14);addText(body,"Eventos registrados",18,NAVY,true);
        JSONArray agenda=array("agenda");
        if(agenda.length()==0)addText(body,"La agenda está vacía.",13,MUTED,false);
        for(int i=0;i<agenda.length();i++){JSONObject a=agenda.optJSONObject(i);if(a==null)continue;LinearLayout card=cardLayout();addText(card,a.optString("Cliente").isEmpty()?a.optString("Tipo"):a.optString("Cliente")+" · "+a.optString("Tipo"),16,NAVY,true);addText(card,a.optString("Fecha y hora")+"\nEstado: "+a.optString("Estado")+"\n"+a.optString("Notas"),13,MUTED,false);if("Pendiente".equals(a.optString("Estado"))){Button done=secondaryButton("Marcar realizado");String id=a.optString("ID Evento");done.setOnClickListener(v->{JSONObject r=request("updateAgendaStatus");put(r,"id",id);put(r,"estado","Realizado");apiRequest(r,(res,err)->{if(success(res,err)){toast("Evento actualizado");refreshData("agenda");}});});card.addView(done,matchWrap());}body.addView(card,matchWrap());}
    }

    private void showExpenses() {
        activeScreen="expenses";buildShell("Gastos",true);
        EditText date=addField("Fecha (AAAA-MM-DD)",today(),InputType.TYPE_CLASS_TEXT);
        Spinner category=addSpinner(new String[]{"Combustible","Mantenimiento","Sueldos","Operación","Estacionamiento","Otros"});
        EditText concept=addField("Concepto del gasto *","",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        EditText amount=addField("Importe ($) *","",InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        Spinner method=addSpinner(new String[]{"Efectivo","Transferencia","Tarjeta","Otro"});
        EditText notes=addField("Notas","",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        Button save=primaryButton("Guardar gasto");save.setOnClickListener(v->{if(concept.getText().toString().trim().isEmpty()||amount.getText().toString().trim().isEmpty()){toast("Completa concepto e importe.");return;}JSONObject r=request("saveExpense");put(r,"fecha",date.getText().toString());put(r,"categoria",category.getSelectedItem().toString());put(r,"concepto",concept.getText().toString());put(r,"importe",amount.getText().toString());put(r,"metodoPago",method.getSelectedItem().toString());put(r,"notas",notes.getText().toString());save.setEnabled(false);apiRequest(r,(res,err)->{save.setEnabled(true);if(success(res,err)){toast("Gasto guardado");refreshData("expenses");}});});body.addView(save,matchWrap());
        addSpace(body,14);addText(body,"Gastos recientes",18,NAVY,true);JSONArray expenses=array("expenses");for(int i=0;i<expenses.length();i++){JSONObject g=expenses.optJSONObject(i);if(g!=null)addInfoCard(g.optString("Concepto"),g.optString("Fecha")+" · "+g.optString("Categoría")+"\n"+money(g.optDouble("Importe",0))+" · "+g.optString("Método de pago"));}
    }

    private void showReports() {
        activeScreen="reports";buildShell("Reportes",true);
        EditText month=addField("Mes a consultar (AAAA-MM)",currentMonth(),InputType.TYPE_CLASS_TEXT);
        LinearLayout resultBox=new LinearLayout(this);resultBox.setOrientation(LinearLayout.VERTICAL);resultBox.setPadding(0,dp(10),0,0);body.addView(resultBox,matchWrap());
        Button load=primaryButton("Consultar reporte");load.setOnClickListener(v->{String m=month.getText().toString().trim();if(!m.matches("\\d{4}-\\d{2}")){toast("Escribe el mes como AAAA-MM, por ejemplo 2026-10.");return;}JSONObject r=request("getReport");put(r,"month",m);load.setEnabled(false);apiRequest(r,(res,err)->{load.setEnabled(true);resultBox.removeAllViews();if(err!=null||!res.optBoolean("ok",true)){addText(resultBox,err!=null?err:res.optString("error","No se pudo cargar el reporte."),14,Color.RED,false);return;}addMetricTo(resultBox,"Ventas del mes",money(res.optDouble("totalSales",0)),"Operaciones: "+res.optInt("transactionCount",0));addMetricTo(resultBox,"Litros vendidos",qty(res.optDouble("totalLiters",0)),"Litros registrados");addMetricTo(resultBox,"Cobros recibidos",money(res.optDouble("totalCollected",0)),"Pagos: "+res.optInt("collectionCount",0));addMetricTo(resultBox,"Gastos",money(res.optDouble("totalExpenses",0)),"Flujo registrado: "+money(res.optDouble("cashFlow",0)));addText(resultBox,"Principales saldos pendientes",17,NAVY,true);JSONArray debtors=res.optJSONArray("topDebtors");if(debtors!=null)for(int i=0;i<debtors.length();i++){JSONObject d=debtors.optJSONObject(i);if(d!=null)addText(resultBox,d.optString("cliente")+" — "+money(d.optDouble("saldo",0)),14,TEXT,false);}addText(resultBox,"Cobros por método",17,NAVY,true);JSONArray methods=res.optJSONArray("paymentMethods");if(methods!=null)for(int i=0;i<methods.length();i++){JSONObject d=methods.optJSONObject(i);if(d!=null)addText(resultBox,d.optString("metodo")+" — "+money(d.optDouble("total",0)),14,TEXT,false);}});});body.addView(load,matchWrap());
    }

    private void refreshData(String screen) {
        activeScreen = screen;
        JSONObject r = request("getAppData");
        apiRequest(r, (res, error) -> {
            if (error != null || !res.optBoolean("ok", true)) {
                settingsMessage = error != null ? error : res.optString("error", "No se pudieron cargar los datos.");
                if (apiUrl.isEmpty() || apiKey.isEmpty()) showSettings(); else {
                    toast("No se pudieron cargar los datos. Revisa la conexión o la clave API.");
                    showSettings();
                }
                return;
            }
            appData = res;
            settingsMessage = "";
            switch (screen) {
                case "clients": showClients(); break;
                case "clientForm": showClientForm(); break;
                case "saleForm": showSaleForm(); break;
                case "credits": showCredits(); break;
                case "agenda": showAgenda(); break;
                case "expenses": showExpenses(); break;
                case "reports": showReports(); break;
                default: showHome(); break;
            }
        });
    }

    private JSONObject request(String action) { JSONObject r=new JSONObject(); try {r.put("action",action);}catch(JSONException ignored){} return r; }
    private void put(JSONObject o,String k,Object v){try{o.put(k,v==null?"":v);}catch(JSONException ignored){}}
    private JSONArray array(String key){return appData==null?new JSONArray():appData.optJSONArray(key)==null?new JSONArray():appData.optJSONArray(key);}

    private void apiRequest(JSONObject request, ApiCallback callback) {
        if (apiUrl == null || apiUrl.isEmpty() || apiKey == null || apiKey.isEmpty()) { mainHandler.post(() -> callback.done(null,"Configura la URL y la clave API.")); return; }
        try { request.put("apiKey",apiKey); } catch(JSONException ignored){}
        executor.execute(() -> {
            HttpURLConnection connection=null;
            JSONObject result=null; String error=null;
            try {
                URL url=new URL(apiUrl);
                connection=(HttpURLConnection)url.openConnection();
                connection.setRequestMethod("POST"); connection.setDoOutput(true); connection.setInstanceFollowRedirects(true);
                connection.setConnectTimeout(20000); connection.setReadTimeout(25000);
                connection.setRequestProperty("Content-Type","application/json; charset=UTF-8");
                byte[] bytes=request.toString().getBytes(StandardCharsets.UTF_8);
                try(OutputStream out=connection.getOutputStream()){out.write(bytes);}
                int code=connection.getResponseCode();
                InputStream stream=(code>=200&&code<400)?connection.getInputStream():connection.getErrorStream();
                if(stream==null)throw new Exception("El servidor respondió HTTP "+code+".");
                StringBuilder sb=new StringBuilder();
                try(BufferedReader reader=new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8))){String line;while((line=reader.readLine())!=null)sb.append(line);}
                String raw=sb.toString().trim();
                try { result=new JSONObject(raw); }
                catch(JSONException ex){throw new Exception("La respuesta no es JSON. Comprueba que pegaste la URL /exec de la API, no la URL de la app web.");}
                if(!result.optBoolean("ok",true))error=result.optString("error","La API rechazó la solicitud.");
            } catch(Exception ex){error=ex.getMessage()==null?"Error de conexión.":ex.getMessage();}
            finally {if(connection!=null)connection.disconnect();}
            final JSONObject response=result; final String message=error;
            mainHandler.post(() -> callback.done(response==null?new JSONObject():response,message));
        });
    }

    private boolean success(JSONObject response,String error){if(error!=null){toast(error);return false;}if(response==null||!response.optBoolean("ok",false)){toast(response==null?"Error inesperado.":response.optString("error","No se pudo completar la operación."));return false;}return true;}
    private void addMetric(String label,String value,String detail){addMetricTo(body,label,value,detail);}
    private void addMetricTo(LinearLayout parent,String label,String value,String detail){LinearLayout card=cardLayout();addText(card,label,13,MUTED,true);addText(card,value,25,NAVY,true);addText(card,detail,12,MUTED,false);parent.addView(card,matchWrap());}
    private void addInfoCard(String title,String detail){LinearLayout card=cardLayout();addText(card,title,16,NAVY,true);addText(card,detail,13,MUTED,false);body.addView(card,matchWrap());}
    private void addAction(String label,Runnable action){Button b=primaryButton(label);b.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);b.setPadding(dp(16),0,dp(12),0);b.setOnClickListener(v->action.run());LinearLayout.LayoutParams p=matchWrap();p.bottomMargin=dp(8);body.addView(b,p);}
    private LinearLayout cardLayout(){LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(14),dp(12),dp(14),dp(14));card.setBackgroundColor(Color.WHITE);LinearLayout.LayoutParams p=matchWrap();p.bottomMargin=dp(10);card.setElevation(dp(1));return card;}
    private EditText addField(String hint,String value,int inputType){EditText e=field(hint,value,inputType);LinearLayout.LayoutParams p=matchWrap();p.bottomMargin=dp(10);body.addView(e,p);return e;}
    private EditText field(String hint,String value,int inputType){EditText e=new EditText(this);e.setSingleLine(inputType!= (InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE));e.setHint(hint);e.setText(value);e.setTextSize(15);e.setInputType(inputType);e.setTextColor(TEXT);e.setHintTextColor(MUTED);e.setPadding(dp(12),dp(10),dp(12),dp(10));e.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(185,195,205)));return e;}
    private Spinner addSpinner(String[] options){Spinner s=addSpinnerTo(body,options);LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)s.getLayoutParams();p.bottomMargin=dp(10);s.setLayoutParams(p);return s;}
    private Spinner addSpinnerTo(LinearLayout parent,String[] options){Spinner s=new Spinner(this);ArrayAdapter<String>a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,options);a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);s.setAdapter(a);LinearLayout.LayoutParams p=matchWrap();p.bottomMargin=dp(8);parent.addView(s,p);return s;}
    private Button primaryButton(String label){Button b=new Button(this);b.setText(label);b.setTextColor(Color.WHITE);b.setAllCaps(false);b.setTextSize(15);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackgroundTintList(ColorStateList.valueOf(GREEN));b.setPadding(dp(12),dp(8),dp(12),dp(8));return b;}
    private Button secondaryButton(String label){Button b=new Button(this);b.setText(label);b.setTextColor(NAVY);b.setAllCaps(false);b.setTextSize(14);b.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(224,233,240)));return b;}
    private Button smallButton(String label){Button b=secondaryButton(label);b.setTextSize(12);b.setPadding(dp(8),0,dp(8),0);return b;}
    private void addText(LinearLayout parent,String text,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(text);t.setTextColor(color);t.setTextSize(size);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(0,dp(3),0,dp(5));t.setLineSpacing(dp(2),1f);parent.addView(t,matchWrap());}
    private void addSpace(LinearLayout parent,int height){View v=new View(this);parent.addView(v,new LinearLayout.LayoutParams(1,dp(height)));}
    private LinearLayout.LayoutParams matchWrap(){return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);}
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
    private String money(double v){return "$"+String.format(Locale.US,"%,.2f",v);}
    private String qty(double v){return String.format(Locale.US,"%,.2f",v);}
    private String today(){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());}
    private String nowDateTime(){return new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(new Date());}
    private String currentMonth(){return new SimpleDateFormat("yyyy-MM",Locale.US).format(new Date());}
}
