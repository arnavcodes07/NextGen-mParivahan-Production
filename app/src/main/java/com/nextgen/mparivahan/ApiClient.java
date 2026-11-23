package com.nextgen.mparivahan;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** HTTP client for the bundled Spring Boot API. Emulator host alias: 10.0.2.2. */
public final class ApiClient {
    // Android Studio emulator. For a physical phone, replace with the PC's LAN IPv4 address.
    public static final String BASE_URL = BuildConfig.API_BASE_URL;
    private static final ExecutorService IO = Executors.newFixedThreadPool(4);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private ApiClient() {}
    public interface Callback { void onResult(boolean success, JSONObject data, String error); }

    public static void login(String mobile, String mpin, Callback cb) {
        JSONObject b=new JSONObject(); try{b.put("mobile",mobile);b.put("mpin",mpin);}catch(Exception e){fail(cb,e);return;} post("/auth/login",b,cb);
    }
    public static void register(String name,String mobile,String email,Callback cb){JSONObject b=new JSONObject();try{b.put("name",name);b.put("mobile",mobile);b.put("email",email);}catch(Exception e){fail(cb,e);return;}post("/auth/register",b,cb);}
    public static void generateMpin(String mobile,Callback cb){JSONObject b=new JSONObject();try{b.put("mobile",mobile);}catch(Exception e){fail(cb,e);return;}post("/auth/generate-mpin",b,cb);}
    public static void vehicle(String registration,Callback cb){get("/vehicles/"+Uri.encode(registration.trim()),cb);}
    public static void licence(String number,Callback cb){get("/licences/"+Uri.encode(number.trim()),cb);}
    public static void profile(String mobile,Callback cb){get("/profile/"+Uri.encode(mobile),cb);}
    public static void updateProfile(String mobile,String name,String email,Callback cb){JSONObject b=new JSONObject();try{b.put("name",name);b.put("email",email);}catch(Exception e){fail(cb,e);return;}put("/profile/"+Uri.encode(mobile),b,cb);}
    public static void submitComplaint(String mobile,String category,String subcategory,String vehicle,String remarks,Callback cb){JSONObject b=new JSONObject();try{b.put("mobile",mobile);b.put("category",category);b.put("subcategory",subcategory);b.put("vehicle",vehicle);b.put("remarks",remarks);}catch(Exception e){fail(cb,e);return;}post("/complaints",b,cb);}
    public static void complaints(String mobile,Callback cb){get("/complaints/"+Uri.encode(mobile),cb);}
    public static void recordTransaction(String mobile,String type,String value,Callback cb){JSONObject b=new JSONObject();try{b.put("mobile",mobile);b.put("type",type);b.put("value",value);}catch(Exception e){fail(cb,e);return;}post("/transactions",b,cb);}
    public static void transactions(String mobile,Callback cb){get("/transactions/"+Uri.encode(mobile),cb);}
    public static void health(Callback cb){get("/health",cb);}
    public static void services(Callback cb){get("/services",cb);}
    public static void nearby(Callback cb){get("/nearby",cb);}
    public static void emergency(Callback cb){get("/emergency",cb);}
    public static void people(Callback cb){get("/people",cb);}

    private static void get(String path,Callback cb){request("GET",path,null,cb);}
    private static void post(String path,JSONObject body,Callback cb){request("POST",path,body,cb);}
    private static void put(String path,JSONObject body,Callback cb){request("PUT",path,body,cb);}
    private static void fail(Callback cb,Exception e){MAIN.post(()->cb.onResult(false,null,e.getMessage()));}
    private static void request(String method,String path,JSONObject body,Callback cb){
        IO.execute(()->{HttpURLConnection c=null;try{
            c=(HttpURLConnection)new URL(BASE_URL+path).openConnection();c.setRequestMethod(method);c.setConnectTimeout(7000);c.setReadTimeout(7000);c.setRequestProperty("Accept","application/json");
            if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=utf-8");try(OutputStream o=c.getOutputStream()){o.write(body.toString().getBytes(StandardCharsets.UTF_8));}}
            int code=c.getResponseCode();InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();String raw=read(in);JSONObject result;
            if(raw.trim().startsWith("[")){result=new JSONObject();result.put("items",new JSONArray(raw));}else result=raw.isEmpty()?new JSONObject():new JSONObject(raw);
            if(code>=200&&code<300){JSONObject r=result;MAIN.post(()->cb.onResult(true,r,null));}
            else {String msg=result.optString("message",result.optString("error","HTTP "+code));MAIN.post(()->cb.onResult(false,result,"HTTP "+code+": "+msg));}
        }catch(Exception e){String msg=e.getClass().getSimpleName()+": "+(e.getMessage()==null?"Cannot reach local API":e.getMessage());MAIN.post(()->cb.onResult(false,null,msg));}finally{if(c!=null)c.disconnect();}});
    }
    private static String read(InputStream in)throws IOException{if(in==null)return "";try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){StringBuilder b=new StringBuilder();String line;while((line=r.readLine())!=null)b.append(line);return b.toString();}}
}
