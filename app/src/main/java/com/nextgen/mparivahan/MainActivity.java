package com.nextgen.mparivahan;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.text.method.PasswordTransformationMethod;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final int BLUE = Color.rgb(0,174,239);
    private static final int DEEP_BLUE = Color.rgb(0,117,170);
    private static final int PALE = Color.rgb(241,248,251);
    private static final int DARK = Color.rgb(25,43,52);
    private static final int MUTED = Color.rgb(100,119,129);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private FrameLayout root;
    private EditText mobile, mpin;
    private TextView category, subCategory, counter;
    private EditText complaintVehicle, complaintRemarks;
    private Uri attachment;
    private LinearLayout drawer;
    private View drawerShade;
    private boolean homeVisible = false;
    private boolean navigationEnabled = false;
    private String currentMobile = "";
    private final ArrayDeque<ScreenSnapshot> navigationStack = new ArrayDeque<>();
    private static final int REQUEST_BACKUP = 45;
    // Android 13+ predictive/system Back integration. Older Android versions
    // continue to use onBackPressed().
    private Object systemBackCallback;

    private static class ScreenSnapshot {
        final FrameLayout view;
        final boolean wasHome;
        ScreenSnapshot(FrameLayout view, boolean wasHome) { this.view = view; this.wasHome = wasHome; }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        if (Build.VERSION.SDK_INT >= 33) {
            android.window.OnBackInvokedCallback callback = this::goBack;
            systemBackCallback = callback;
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback);
        }
        // Show the supplied branded splash asset first, then open the login screen.
        // The previous build defined showSplash() but never called it from onCreate(),
        // which is why the splash screen was skipped entirely.
        showSplash();
    }

    private void showLoginSafely() {
        try {
            showLogin();
        } catch (RuntimeException ex) {
            // Keep the app usable instead of leaving an empty window after UI setup fails.
            base();
            LinearLayout fallback = new LinearLayout(this);
            fallback.setOrientation(LinearLayout.VERTICAL);
            fallback.setGravity(Gravity.CENTER);
            fallback.setPadding(dp(24), dp(24), dp(24), dp(24));
            TextView title = text("NextGen mParivahan", 25, DARK, true);
            TextView message = text("Login screen could not be loaded. Tap to retry.", 16, DARK, false);
            message.setPadding(0, dp(16), 0, dp(16));
            TextView retry = text("Retry", 18, Color.WHITE, true);
            retry.setGravity(Gravity.CENTER);
            retry.setBackground(shape(BLUE, 16));
            fallback.addView(title);
            fallback.addView(message);
            fallback.addView(retry, new LinearLayout.LayoutParams(-1, dp(56)));
            root.addView(fallback, new FrameLayout.LayoutParams(-1, -1));
            retry.setOnClickListener(v -> showLoginSafely());
        }
    }

    private void base() {
        if (navigationEnabled && root != null) {
            if (navigationStack.size() >= 20) navigationStack.removeLast();
            navigationStack.push(new ScreenSnapshot(root, homeVisible));
        }
        homeVisible = false;
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.WHITE);
        root.setAlpha(0f);
        setContentView(root);
        root.animate().alpha(1f).setDuration(170).start();
    }

    // Ignore accidental rapid repeated taps so two screens cannot be pushed in one gesture.
    private void navigateOnce(View source, Runnable action) {
        long now = android.os.SystemClock.elapsedRealtime();
        Object previous = source.getTag();
        if (previous instanceof Long && now - (Long) previous < 350L) return;
        source.setTag(now);
        action.run();
    }

    private String activeMobile() {
        if (currentMobile == null || currentMobile.isEmpty()) {
            currentMobile = getSharedPreferences("session", MODE_PRIVATE).getString("mobile", "");
        }
        return currentMobile;
    }

    private void showSplash() {
        navigationStack.clear();
        navigationEnabled = false;
        base();
        root.setBackgroundColor(BLUE);
        ImageView img = new ImageView(this);
        img.setImageResource(R.drawable.splash);
        // Preserve the original splash artwork proportions; never stretch or crop it.
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);
        img.setContentDescription("NextGen mParivahan splash screen");
        root.addView(img, new FrameLayout.LayoutParams(-1,-1));
        handler.postDelayed(this::showLoginSafely, 1400);
    }

    private TextView text(String s, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, sp(size)); t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setIncludeFontPadding(true);
        t.setTextScaleX(1.0f);
        t.setHorizontallyScrolling(false);
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            t.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_HIGH_QUALITY);
            t.setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_NORMAL);
        }
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private GradientDrawable shape(int color, float radius) {
        GradientDrawable g = new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp((int)radius)); return g;
    }
    private GradientDrawable stroke(int color, int stroke, float radius) {
        GradientDrawable g = new GradientDrawable(); g.setColor(Color.WHITE); g.setCornerRadius(dp((int)radius)); g.setStroke(dp(stroke), color); return g;
    }
    // Scale layout dimensions gently against the available phone width. 393dp is
    // the reference width; clamping prevents extreme sizing on compact phones/tablets.
    private float uiScale() {
        android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
        float widthDp = metrics.widthPixels / metrics.density;
        return Math.max(0.88f, Math.min(1.08f, widthDp / 393f));
    }

    private float sp(float size) {
        return Math.max(10.5f, size * uiScale());
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density * uiScale());
    }

    // Keep four columns on normal phones; use fewer columns on compact screens
    // so labels and touch targets do not become cramped or clipped.
    private int gridColumnCount() {
        float widthDp = getResources().getDisplayMetrics().widthPixels
                / getResources().getDisplayMetrics().density;
        if (widthDp < 350f) return 2;
        if (widthDp < 390f) return 3;
        return 4;
    }


    private EditText field(String hint, int max, boolean password) {
        EditText e = new EditText(this);
        e.setHint(hint); e.setHintTextColor(Color.rgb(110,115,120));
        e.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, sp(17)); e.setTextColor(DARK); e.setSingleLine(true);
        e.setPadding(dp(18),0,dp(18),0); e.setBackground(shape(PALE,16));
        e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | (password ? android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD : android.text.InputType.TYPE_CLASS_NUMBER));
        if (password) e.setTransformationMethod(PasswordTransformationMethod.getInstance());
        e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(max)});
        return e;
    }

    private void showLogin() {
        navigationStack.clear();
        navigationEnabled = false;
        base();
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL); page.setPadding(dp(22),dp(20),dp(22),dp(24));
        scroll.addView(page); root.addView(scroll,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL); header.setPadding(0,0,0,dp(18));
        TextView back = text("‹",40,DARK,false); header.addView(back,new LinearLayout.LayoutParams(dp(48),dp(55))); back.setOnClickListener(v->finish());
        ImageView logo = new ImageView(this); logo.setImageResource(R.drawable.logo); logo.setScaleType(ImageView.ScaleType.FIT_CENTER); logo.setAdjustViewBounds(true);
        // Use the supplied reference only for its logo/header region, not as a full-screen background.
        header.addView(logo,new LinearLayout.LayoutParams(0,dp(75),1));
        page.addView(header);

        TextView title=text("Sign in to\nYour Account !",34,Color.BLACK,true); title.setPadding(0,dp(15),0,dp(26)); page.addView(title);

        mobile=field("Mobile No.",10,false); page.addView(mobile,new LinearLayout.LayoutParams(-1,dp(64)));
        Space gap1=new Space(this); page.addView(gap1,new LinearLayout.LayoutParams(1,dp(14)));

        LinearLayout pinRow=new LinearLayout(this); pinRow.setGravity(Gravity.CENTER_VERTICAL);
        mpin=field("4 digits security MPIN",4,true); pinRow.addView(mpin,new LinearLayout.LayoutParams(0,dp(64),1));
        TextView eye=text("◉",20,Color.GRAY,false); eye.setGravity(Gravity.CENTER); eye.setBackground(shape(PALE,16));
        pinRow.addView(eye,new LinearLayout.LayoutParams(dp(58),dp(64))); page.addView(pinRow);
        eye.setOnClickListener(v->{ if(mpin.getTransformationMethod()==null) mpin.setTransformationMethod(PasswordTransformationMethod.getInstance()); else mpin.setTransformationMethod(null); mpin.setSelection(mpin.length()); });

        LinearLayout links=new LinearLayout(this); links.setGravity(Gravity.CENTER_VERTICAL); links.setPadding(0,dp(12),0,dp(8));
        TextView other=text("Use another account",16,BLUE,false); TextView forgot=text("Forgot security MPIN",16,BLUE,false);
        links.addView(other,new LinearLayout.LayoutParams(0,dp(50),1)); links.addView(forgot,new LinearLayout.LayoutParams(0,dp(50),1)); page.addView(links);
        other.setOnClickListener(v->{mobile.setText("");mpin.setText("");mobile.requestFocus();}); forgot.setOnClickListener(v->forgotPin());

        TextView sign=text("Sign in with MPIN",18,Color.WHITE,true); sign.setGravity(Gravity.CENTER); sign.setBackground(shape(BLUE,16)); page.addView(sign,new LinearLayout.LayoutParams(-1,dp(64))); sign.setOnClickListener(v->login());
        Space gap2=new Space(this); page.addView(gap2,new LinearLayout.LayoutParams(1,dp(12)));
        TextView gen=text("Generate MPIN",18,BLUE,true); gen.setGravity(Gravity.CENTER); gen.setBackground(stroke(BLUE,2,16)); page.addView(gen,new LinearLayout.LayoutParams(-1,dp(64))); gen.setOnClickListener(v->generateMpin());

        LinearLayout signup=new LinearLayout(this); signup.setGravity(Gravity.CENTER); signup.setPadding(0,dp(50),0,dp(10));
        TextView a=text("Don't have an account? ",16,Color.DKGRAY,false), b=text("Sign Up",17,BLUE,true); signup.addView(a); signup.addView(b); page.addView(signup); signup.setOnClickListener(v->signupDialog());
    }

    private void login() {
        String m=mobile.getText().toString().trim(), p=mpin.getText().toString().trim();
        if(!m.matches("\\d{10}")){toast("Mobile number must be exactly 10 digits");return;}
        if(!p.matches("\\d{4}")){toast("MPIN must be exactly 4 digits");return;}
        Toast.makeText(this, "Connecting to local API...", Toast.LENGTH_SHORT).show();
        ApiClient.login(m, p, (ok, data, error) -> {
            if (ok) {
                currentMobile = m;
                getSharedPreferences("session", MODE_PRIVATE).edit().putString("mobile", m).apply();
                toast("Connected to local API");
                showHome();
            } else {
                toast(error != null && error.startsWith("HTTP 401")
                        ? "Invalid mobile number or MPIN" : "Local API unavailable: " + error);
            }
        });
    }
    private void generateMpin(){
        String m=mobile.getText().toString().trim();
        if(!m.matches("\\d{10}")){toast("Enter exactly 10 digits first");return;}
        ApiClient.generateMpin(m, (ok, data, error) -> {
            if (ok) { String pin=data.optString("mpin", ""); mpin.setText(pin); mpin.setSelection(mpin.length()); toast("MPIN generated by local API"); }
            else toast("Could not generate MPIN: " + error);
        });
    }
    private void forgotPin(){
        EditText e=field("10 digit mobile number",10,false);
        new AlertDialog.Builder(this).setTitle("Forgot security MPIN").setView(e).setNegativeButton("Cancel",null).setPositiveButton("Generate",(d,w)->{
            String m=e.getText().toString(); if(!m.matches("\\d{10}")){toast("Mobile must be exactly 10 digits");return;} ApiClient.generateMpin(m,(ok,data,error)->{if(ok){mobile.setText(m);mpin.setText(data.optString("mpin",""));toast("MPIN generated by local API");}else toast("Could not generate MPIN: "+error);});
        }).show();
    }
    private void signupDialog(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(20),0,dp(20),0);
        EditText n=new EditText(this);n.setHint("Name"); EditText em=new EditText(this);em.setHint("Email"); EditText m=field("Mobile",10,false);
        box.addView(n);box.addView(em);box.addView(m);
        new AlertDialog.Builder(this).setTitle("Create account").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Create",(d,w)->{
            if(!m.getText().toString().matches("\\d{10}")){toast("Mobile must be exactly 10 digits");return;} ApiClient.register(n.getText().toString(),m.getText().toString(),em.getText().toString(),(ok,data,error)->{if(ok)toast("Account created. API MPIN: "+data.optString("mpin",""));else toast("Registration failed: "+error);});
        }).show();
    }

    private void showHome(){
        navigationStack.clear();
        navigationEnabled = false;
        base(); homeVisible=true;
        LinearLayout outer=new LinearLayout(this); outer.setOrientation(LinearLayout.VERTICAL); outer.setBackgroundColor(0xFFF7FBFD);
        root.addView(outer,new FrameLayout.LayoutParams(-1,-1));
        outer.addView(header("NextGen mParivahan",true),new LinearLayout.LayoutParams(-1,dp(66)));
        LinearLayout search=new LinearLayout(this); search.setGravity(Gravity.CENTER_VERTICAL); search.setPadding(dp(12),dp(9),dp(12),dp(9)); search.setBackgroundColor(0xFFEAF7FB);
        TextView selector=text("Vehicle  ▾",14,Color.WHITE,true); selector.setGravity(Gravity.CENTER); selector.setBackground(shape(BLUE,14)); selector.setElevation(dp(2));
        EditText vehicle=field("Enter Vehicle No.",20,false); vehicle.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS); vehicle.setBackground(shape(Color.WHITE,14)); vehicle.setSingleLine(true); vehicle.setPadding(dp(13),0,dp(6),0); vehicle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, sp(14)); vehicle.setElevation(dp(2));
        TextView go=text("⌕",32,0xFF7B9BA8,false); go.setGravity(Gravity.CENTER); go.setBackground(shape(Color.WHITE,14)); go.setElevation(dp(2));
        search.addView(selector,new LinearLayout.LayoutParams(dp(108),dp(48))); LinearLayout.LayoutParams searchFieldLp=new LinearLayout.LayoutParams(0,dp(48),1); searchFieldLp.setMargins(dp(5),0,dp(5),0); search.addView(vehicle,searchFieldLp); search.addView(go,new LinearLayout.LayoutParams(dp(44),dp(48))); outer.addView(search);
        selector.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Search category").setItems(new String[]{"Vehicle","Driving Licence"},(d,w)->{selector.setText(w==0?"Vehicle  ▾":"Licence  ▾");vehicle.setHint(w==0?"Enter Vehicle No.":"Enter DL Number");vehicle.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);}).show());
        go.setOnClickListener(v->navigateOnce(v,()->{String q=vehicle.getText().toString().trim();if(q.isEmpty()){toast("Enter search number");return;}boolean isVehicle=selector.getText().toString().startsWith("Vehicle");ApiClient.recordTransaction(activeMobile(),isVehicle?"Vehicle Search":"Driving Licence Search",q,(ok,data,error)->{if(!ok)toast("Could not save activity to API: "+error);});showSearchResult(isVehicle?"Vehicle Search":"Driving Licence Search",q);}));
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(false); scroll.setClipToPadding(false); LinearLayout page=new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL); page.setPadding(dp(12),dp(8),dp(12),dp(24)); scroll.addView(page); outer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView serviceCaption=text("QUICK ACCESS",11,DEEP_BLUE,true); serviceCaption.setLetterSpacing(0.12f); serviceCaption.setPadding(dp(5),dp(8),0,dp(4)); page.addView(serviceCaption);
        addQuickAccess(page);
        addSection(page,"About NextGen mParivahan",new String[][]{{"▣","Virtual RC"},{"◈","Virtual DL"},{"▤","Shared RC"},{"▥","Received RC"},{"⌕","RC Search"},{"⌕","DL Search"}},new Runnable[]{()->showDataPage("Virtual RC",new String[]{"View digital RC","Download / share RC","Vehicle details","Validity information"}),()->showDataPage("Virtual DL",new String[]{"View digital DL","Download / share DL","Licence details","Validity information"}),()->showDataPage("Shared RC",new String[]{"Shared vehicle RC","Received requests","Share status","Request history"}),()->showDataPage("Received RC",new String[]{"Received RC documents","Open document","Share document","Document history"}),this::vehicleSearch,this::licenceSearch});
        addSection(page,"About Vehicle",new String[][]{{"▧","Duplicate RC"},{"▦","Hypothecation\nTermination"},{"♙","Transfer of\nOwnership"},{"▤","RC Particulars"},{"⌖","Change of\nAddress"},{"▣","HP Endorsement"},{"▥","Hypothecation\nContinuation"},{"▤","No Objection\nCertificate (NOC)"}},new Runnable[]{()->showDataPage("Duplicate RC",new String[]{"Application status","Required documents","Fee information","Service availability"}),()->showDataPage("Hypothecation Termination",new String[]{"Application details","Required documents","Status tracking","Fee information"}),()->showDataPage("Transfer of Ownership",new String[]{"Ownership transfer","Required documents","Application status","Fee information"}),()->showDataPage("RC Particulars",new String[]{"Vehicle details","Registration details","Owner details","Status"}),()->showDataPage("Change of Address",new String[]{"Current address","New address","Required documents","Application status"}),()->showDataPage("HP Endorsement",new String[]{"Hypothecation details","Application status","Required documents","Fee information"}),()->showDataPage("Hypothecation Continuation",new String[]{"Continuation details","Lender information","Required documents","Status"}),()->showDataPage("No Objection Certificate",new String[]{"NOC status","Application details","Required documents","Download / share"})});
        bottomBar(outer);
        navigationEnabled = true;
        ApiClient.services((ok,data,error)->{ if(!ok) toast("Services API unavailable: " + error); });
    }

    private View header(String title, boolean menu){
        LinearLayout h=new LinearLayout(this); h.setGravity(Gravity.CENTER_VERTICAL); h.setPadding(dp(5),0,dp(8),0); h.setBackgroundColor(Color.WHITE);
        h.setElevation(dp(3));
        if(menu){TextView m=text("☰",25,0xFF315D70,true);m.setGravity(Gravity.CENTER);h.addView(m,new LinearLayout.LayoutParams(dp(48),dp(60)));m.setOnClickListener(v->navigateOnce(v,this::openDrawer));}
        ImageView logo=new ImageView(this); logo.setImageResource(R.drawable.logo); logo.setScaleType(ImageView.ScaleType.FIT_CENTER); logo.setAdjustViewBounds(true); h.addView(logo,new LinearLayout.LayoutParams(0,dp(52),1));
        TextView sos=text("SOS",11,Color.RED,true);sos.setGravity(Gravity.CENTER);sos.setBackground(stroke(0xFFFFD6D6,1,14));h.addView(sos,new LinearLayout.LayoutParams(dp(48),dp(38)));sos.setOnClickListener(v->navigateOnce(v,this::showEmergency));
        ImageView bell=new ImageView(this);bell.setImageResource(R.drawable.ic_nav_notifications);bell.setScaleType(ImageView.ScaleType.CENTER_INSIDE);bell.setPadding(dp(9),dp(9),dp(9),dp(9));h.addView(bell,new LinearLayout.LayoutParams(dp(44),dp(58)));bell.setOnClickListener(v->navigateOnce(v,()->showDataPage("Notifications",new String[]{"Vehicle document reminder","Road safety advisory","Demo service update"})));
        return h;
    }
    private void addQuickAccess(LinearLayout page) {
        String[][] items = {
                {"🚘", "Transport Services"},
                {"▤", "My Transactions"},
                {"♧", "Citizen Sentinel"},
                {"▣", "Informational Services"}
        };
        Runnable[] actions = {
                () -> showDataPage("Transport Services", new String[]{"Vehicle Registration", "Driving Licence", "Permit Services", "Challan Services", "Tax / Fee Information", "RC Search", "DL Search"}),
                this::showTransactions,
                this::showCitizen,
                () -> showDataPage("Information Services", new String[]{"Traffic Rules", "Road Safety", "RTO Information", "Vehicle Information", "DL Information", "Demo People Directory", "Citizen Help"})
        };
        float widthDp = getResources().getDisplayMetrics().widthPixels
                / getResources().getDisplayMetrics().density;
        int columns = widthDp < 390f ? 2 : 4;
        for (int start = 0; start < items.length; start += columns) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER);
            page.addView(row, new LinearLayout.LayoutParams(-1, dp(146)));
            for (int j = start; j < Math.min(start + columns, items.length); j++) {
                addService(row, items[j][0], items[j][1], actions[j]);
            }
        }
    }

    private void addService(LinearLayout row,String icon,String name,Runnable action){
        LinearLayout c=tile(icon,name,action);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);p.setMargins(dp(3),dp(3),dp(3),dp(3));row.addView(c,p);
    }
    private int referenceIconFor(String label){
        String k=label.replace("\n"," ").trim().toLowerCase(Locale.US).replaceAll("\\s+"," ");
        String resource=null;
        if(k.equals("transport services"))resource="icon_transport";
        else if(k.equals("my transactions"))resource="icon_transactions";
        else if(k.equals("citizen sentinel"))resource="icon_citizen";
        else if(k.equals("informational services"))resource="icon_information";
        else if(k.equals("virtual rc"))resource="icon_virtual_rc";
        else if(k.equals("virtual dl"))resource="icon_virtual_dl";
        else if(k.equals("shared rc"))resource="icon_shared_rc";
        else if(k.equals("received rc"))resource="icon_received_rc";
        else if(k.equals("rc search"))resource="icon_rc_search";
        else if(k.equals("dl search"))resource="icon_dl_search";
        else if(k.equals("duplicate rc"))resource="icon_duplicate_rc";
        else if(k.equals("hypothecation termination"))resource="icon_hypothecation_termination";
        else if(k.equals("transfer of ownership"))resource="icon_transfer_ownership";
        else if(k.equals("rc particulars"))resource="icon_rc_particulars";
        else if(k.equals("change of address"))resource="icon_change_address";
        else if(k.equals("hp endorsement"))resource="icon_hp_endorsement";
        else if(k.equals("hypothecation continuation"))resource="icon_hypothecation_continuation";
        else if(k.startsWith("no objection certificate"))resource="icon_noc";
        return resource==null?0:getResources().getIdentifier(resource,"drawable",getPackageName());
    }
    private LinearLayout tile(String icon,String name,Runnable action){
        LinearLayout c=new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER);
        c.setPadding(dp(5),dp(7),dp(5),dp(7));
        c.setBackground(shape(Color.WHITE,16));
        c.setElevation(dp(2));

        int ref=referenceIconFor(name);
        FrameLayout iconBox=new FrameLayout(this);
        iconBox.setBackground(shape(0xFFF1F8FB,16));
        ImageView image=new ImageView(this);
        if(ref!=0){
            image.setImageResource(ref);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setPadding(dp(3),dp(3),dp(3),dp(3));
            iconBox.addView(image,new FrameLayout.LayoutParams(-1,-1));
        } else {
            TextView glyph=text(icon,30,DEEP_BLUE,true);
            glyph.setGravity(Gravity.CENTER);
            iconBox.addView(glyph,new FrameLayout.LayoutParams(-1,-1));
        }
        LinearLayout.LayoutParams iconLp=new LinearLayout.LayoutParams(dp(56),dp(56));
        iconLp.gravity=Gravity.CENTER;
        c.addView(iconBox,iconLp);

        TextView n=text(name.replace("\n"," ").replace("\\n"," "),12,DARK,true);
        n.setGravity(Gravity.CENTER);
        n.setMaxLines(3);
        n.setMinHeight(dp(52));
        n.setIncludeFontPadding(true);
        n.setLineSpacing(dp(2),1.04f);
        n.setPadding(dp(2),dp(5),dp(2),dp(2));
        n.setEllipsize(null);
        n.setHorizontallyScrolling(false);
        c.addView(n,new LinearLayout.LayoutParams(-1,-2));
        c.setMinimumHeight(dp(136));
        c.setOnClickListener(v->navigateOnce(v,action));
        c.setFocusable(true);
        c.setClickable(true);
        return c;
    }
    private void addSection(LinearLayout page,String title,String[][] data,Runnable[] actions){
        TextView h=text(title,18,DARK,true);h.setPadding(dp(4),dp(18),0,dp(7));page.addView(h);
        View accent=new View(this);accent.setBackground(shape(BLUE,3));LinearLayout.LayoutParams accentLp=new LinearLayout.LayoutParams(dp(42),dp(3));accentLp.setMargins(dp(4),0,0,dp(7));page.addView(accent,accentLp);
        int columns=gridColumnCount();
        for(int start=0;start<data.length;start+=columns){
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER);row.setPadding(0,dp(3),0,dp(3));
            page.addView(row,new LinearLayout.LayoutParams(-1,dp(146)));
            for(int j=start;j<Math.min(start+columns,data.length);j++){
                int k=j;LinearLayout.LayoutParams tileLp=new LinearLayout.LayoutParams(0,-1,1);
                tileLp.setMargins(dp(3),dp(3),dp(3),dp(3));
                row.addView(tile(data[j][0],data[j][1],actions[k]),tileLp);
            }
            int remaining = columns - Math.min(columns, data.length - start);
            for (int blank = 0; blank < remaining; blank++) {
                LinearLayout.LayoutParams spacerLp = new LinearLayout.LayoutParams(0, -1, 1);
                spacerLp.setMargins(dp(3), dp(3), dp(3), dp(3));
                row.addView(new View(this), spacerLp);
            }
        }
    }

    private void showDataPage(String title,String[] items){
        base();LinearLayout outer=new LinearLayout(this);outer.setOrientation(LinearLayout.VERTICAL);outer.setBackgroundColor(0xFFF7FBFD);root.addView(outer,new FrameLayout.LayoutParams(-1,-1));outer.addView(subHeader(title),new LinearLayout.LayoutParams(-1,dp(64)));
        ScrollView scroll=new ScrollView(this);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(dp(16),dp(12),dp(16),dp(24));scroll.addView(list);outer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        for(String item:items){LinearLayout card=new LinearLayout(this);card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(dp(16),dp(9),dp(12),dp(9));card.setBackground(shape(Color.WHITE,16));card.setElevation(dp(3));card.setMinimumHeight(dp(64));TextView n=text(item,15,DARK,true);n.setMinHeight(dp(44));n.setMaxLines(3);card.addView(n,new LinearLayout.LayoutParams(0,-2,1));TextView arrow=text("›",28,0xFF53666D,false);arrow.setGravity(Gravity.CENTER);card.addView(arrow,new LinearLayout.LayoutParams(dp(30),dp(48)));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(10));list.addView(card,lp);card.setOnClickListener(v->navigateOnce(v,()->openServiceItem(item)));}
        bottomBar(outer);
    }
    private void openServiceItem(String item){
        String key=item.toLowerCase(Locale.US);
        if(key.contains("demo people directory")){showPeopleDirectory();return;}
        if(key.contains("vehicle search")||key.equals("rc search")||key.contains("vehicle registration")||key.equals("rc particulars")){vehicleSearch();return;}
        if(key.contains("driving licence search")||key.equals("dl search")||key.equals("driving licence")){licenceSearch();return;}
        if(key.contains("complaint tracking")||key.contains("raise a complaint")){showComplaint();return;}
        if(key.contains("transaction history")){showTransactions();return;}
        showExpanded(item);
    }

    private void showPeopleDirectory(){
        base(); LinearLayout outer=new LinearLayout(this); outer.setOrientation(LinearLayout.VERTICAL); root.addView(outer,new FrameLayout.LayoutParams(-1,-1));
        outer.addView(subHeader("API People Directory"),new LinearLayout.LayoutParams(-1,dp(64)));
        TextView note=text("Fictional sample records loaded from the local Spring Boot API",13,Color.DKGRAY,false); note.setPadding(dp(16),dp(6),dp(16),dp(10)); outer.addView(note);
        ScrollView scroll=new ScrollView(this); LinearLayout list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); list.setPadding(dp(14),dp(4),dp(14),dp(24)); scroll.addView(list); outer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView loading=text("Loading API records...",15,DARK,false); list.addView(loading);
        ApiClient.people((ok,data,error)->{list.removeAllViews(); if(!ok){list.addView(text("API error: "+error,14,DARK,false));return;} JSONArray rows=data.optJSONArray("items"); if(rows==null||rows.length()==0){list.addView(text("No records returned by API",14,DARK,false));return;} for(int i=0;i<rows.length();i++){JSONObject person=rows.optJSONObject(i);if(person==null)continue;String owner=person.optString("owner","Unknown"),reg=person.optString("registration","");LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(16),dp(12),dp(16),dp(12));card.setBackground(shape(PALE,14));card.addView(text(owner,17,DARK,true));card.addView(text("Vehicle registration: "+reg,14,Color.DKGRAY,false));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(8));list.addView(card,lp);card.setOnClickListener(v->navigateOnce(v,()->showSearchResult("Vehicle Search",reg)));}});
        bottomBar(outer);
    }
    private void showExpanded(String title){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(8),dp(4),dp(8),dp(4));
        String[] rows=demoDetails(title);
        for(String line:rows){
            TextView t=text(line,15,DARK,false);t.setGravity(Gravity.TOP);t.setPadding(0,dp(7),0,dp(7));box.addView(t);
            View divider=new View(this);divider.setBackgroundColor(0xFFE5EDF0);box.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));
        }
        ScrollView scroll=new ScrollView(this);scroll.addView(box);
        new AlertDialog.Builder(this).setTitle(title).setView(scroll).setPositiveButton("Close",null).show();
    }

    // Pre-filled, fictional records keep every demo screen useful while offline.
    // They are examples only and are not sourced from government systems.
    private String[] demoDetails(String title){
        switch(title){
            case "Virtual RC": return new String[]{"DEMO DOCUMENT • NOT AN OFFICIAL RC","Registration: DL01AB1000","Registered owner: Aarav Sharma","Vehicle: Maruti Swift • Petrol","Registered at: New Delhi, Delhi","Registration status: Active (sample)","Insurance: Sample policy • Valid through 31 Dec 2026","PUCC: Sample certificate • Valid through 15 Jan 2027","Chassis / engine numbers: Hidden in demo mode","Actions: View details • Share preview • Download sample"};
            case "Virtual DL": return new String[]{"DEMO DOCUMENT • NOT AN OFFICIAL LICENCE","Licence holder: Aarav Sharma","Licence number: DL-DEMO-2020-001","Class: MCWG, LMV","Issuing authority: Delhi (sample)","Issue date: 12 Jun 2020","Valid through: 11 Jun 2040","Status: Active (sample)","Actions: View details • Share preview • Download sample"};
            case "Shared RC": return new String[]{"SAMPLE SHARING CENTRE","Shared with: Priya Verma • 98******21","Vehicle: DL01AB1000 • Maruti Swift","Shared on: 10 Nov 2026","Permission: View-only","Request status: Accepted (demo)","You can revoke this sample share from the document controls."};
            case "Received RC": return new String[]{"SAMPLE RECEIVED DOCUMENTS","From: Rahul Singh • MH01AB1001","Vehicle: Hyundai i20","Received: 08 Nov 2026","Access: View-only preview","Document status: Available (demo)","No real registration certificate is stored in this offline app."};
            case "RC Search": case "Vehicle Search": return new String[]{"SAMPLE VEHICLE RECORD","Registration: DL01AB1000","Owner: Aarav Sharma","Vehicle: Maruti Swift • Petrol","Registered city: New Delhi","State: Delhi","Status: Active (sample)","Try MH01AB1001, UP01AB1002, HR01AB1003 or KA01AB1005.","All owner and vehicle details shown here are fictional demo data."};
            case "DL Search": case "Driving Licence Search": return new String[]{"SAMPLE DRIVING LICENCE RECORD","Licence holder: Priya Verma","Licence number: DL-DEMO-2021-014","Vehicle class: LMV, MCWG","Issuing state: Delhi","Validity: 01 Mar 2021 – 28 Feb 2041","Status: Active (sample)","Enter a real licence number only when connected to an authorised service."};
            case "My Profile": return new String[]{"PROFILE PREVIEW","Name: Arnav Sharma","Mobile: 9560559548","Email: memaverickarnav@gmail.com","State: Delhi","Profile type: Local demo account","Use Edit Profile to update the information saved on this device."};
            case "Traffic Rules": return new String[]{"ROAD SAFETY INFORMATION","Always wear a helmet on two-wheelers and seat belts in cars.","Do not use a handheld phone while driving.","Follow posted speed limits and traffic signals.","Never drive under the influence of alcohol.","Keep RC, insurance, PUC and driving licence valid.","This is general awareness information, not legal advice."};
            case "Road Safety": return new String[]{"ROAD SAFETY CHECKLIST","Before travel: check tyres, brakes, lights and mirrors.","Keep a safe distance and use indicators.","Give way to pedestrians and emergency vehicles.","Avoid overspeeding and distracted driving.","For emergencies in India, call 112."};
            case "RTO Information": return new String[]{"SAMPLE RTO DIRECTORY","Delhi Transport Department • Civic Centre, Minto Road, New Delhi","VAHAN / SARATHI services: registration and licence workflows","Office timings and counters can change; verify before visiting.","This offline sample does not verify live office availability."};
            case "Pollution checking centre": return new String[]{"SAMPLE NEARBY SERVICE","Green Check PUC Centre","Example address: Connaught Place, New Delhi","Services: PUC emission test • Certificate renewal","Hours: 09:00–18:00 (sample only)","Location and contact are illustrative, not live listings."};
            case "RTO": return new String[]{"SAMPLE NEARBY OFFICE","Regional Transport Office – Delhi (sample listing)","Example area: Mall Road, Delhi","Services: Registration • Licence • Permits","Check the official transport department for current address and timings."};
            case "Fuel pump": return new String[]{"SAMPLE FUEL STATION","City Fuel Point","Example area: ITO, New Delhi","Fuel: Petrol • Diesel • CNG (availability not live)","Opening hours: 06:00–23:00 (sample only)."};
            case "Hospital": return new String[]{"EMERGENCY CARE DIRECTORY","For immediate emergency assistance in India call 112.","Nearest hospital results require live location and verified map data.","This sample screen does not claim to identify the nearest facility."};
            case "Emergency contacts": return new String[]{"INDIA EMERGENCY CONTACTS","112 – Unified emergency response","108 – Ambulance service (availability varies by state)","1033 – National Highways helpline","If someone is in immediate danger, call 112 now.","This app's SOS button does not automatically contact emergency services."};
            case "Call 102": return new String[]{"AMBULANCE INFORMATION","102 ambulance service availability varies by state.","For a general emergency in India, call 112.","This demo does not place a call automatically."};
            case "Report traffic violation": return new String[]{"CITIZEN REPORTING","You can create a local demo complaint using Raise a Complaint.","Sample categories: Wrong parking • Overspeeding • Signal violation","Do not upload sensitive personal information.","No report is sent to police or a government portal in offline mode."};
            case "Citizen alerts": return new String[]{"SAMPLE CITIZEN ALERTS","Road safety awareness drive – 22 Nov 2026","Vehicle document check reminder – 30 Nov 2026","Winter fog advisory: use low beam and maintain distance.","These are sample in-app notices, not official live alerts."};
            case "Complaint tracking": return new String[]{"DEMO COMPLAINT TRACKING","CMP-DEMO-2026-001 • Wrong Parking • Under review","CMP-DEMO-2026-002 • Signal Violation • Submitted","Open My Transactions to view local complaint entries.","Tracking statuses are simulated and not sent to authorities."};
            case "Application status": return new String[]{"SAMPLE APPLICATION","Application ID: NG-DEMO-2026-00421","Applicant: Aarav Sharma","Service: Duplicate Registration Certificate","Submitted: 14 Nov 2026","Status: Documents verified (simulated)","Next step: Sample fee payment pending","No real application or payment is created."};
            case "Required documents": return new String[]{"DOCUMENT CHECKLIST (SAMPLE)","Identity proof","Address proof","Existing RC / vehicle details","Insurance and valid PUC, where applicable","Additional documents may be required by the relevant RTO."};
            case "Fee information": return new String[]{"FEE INFORMATION","Fees depend on vehicle class, state rules and service type.","This demo intentionally does not show a payable amount.","Check the official Parivahan portal or your RTO for current fees.","No payment gateway is connected."};
            default: return new String[]{"DEMO SERVICE DETAILS","Service: "+title,"Sample reference: NG-DEMO-2026-"+String.format(Locale.US,"%03d",Math.abs(title.hashCode())%1000),"Applicant: Aarav Sharma","Application date: 14 Nov 2026","Status: Sample record – Ready for review","Required action: Review the checklist and verify details.","This is fictional offline demonstration data. No request is submitted to a government system."};
        }
    }

    private void openDrawer(){
        if (drawer != null || root == null) return;

        // Shade is inserted first; the drawer is inserted above it so all menu
        // rows remain tappable and tapping outside reliably dismisses the panel.
        drawerShade = new View(this);
        drawerShade.setBackgroundColor(0x66000000);
        drawerShade.setClickable(true);
        root.addView(drawerShade, new FrameLayout.LayoutParams(-1, -1));
        drawerShade.setOnClickListener(v -> closeDrawer());

        drawer = new LinearLayout(this);
        drawer.setOrientation(LinearLayout.VERTICAL);
        // Keep the panel flush with the top edge of the app content. The previous
        // 20dp top inset made the drawer look detached from the app header.
        drawer.setPadding(dp(12), dp(4), dp(12), dp(8));
        drawer.setBackgroundColor(Color.WHITE);
        drawer.setElevation(dp(18));
        drawer.setFocusableInTouchMode(true);
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int drawerWidth = Math.max(1, Math.min(dp(320), screenWidth - dp(24)));
        FrameLayout.LayoutParams drawerParams = new FrameLayout.LayoutParams(
                drawerWidth, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.START);
        root.addView(drawer, drawerParams);

        LinearLayout profile = new LinearLayout(this);
        profile.setGravity(Gravity.CENTER_VERTICAL);
        profile.setPadding(dp(10), dp(8), dp(8), dp(12));
        profile.setBackground(shape(0xFFEAF5F8, 16));
        ImageView avatar = new ImageView(this);
        avatar.setImageResource(R.drawable.avatar);
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        profile.addView(avatar, new LinearLayout.LayoutParams(dp(54), dp(54)));
        LinearLayout identity = new LinearLayout(this);
        identity.setOrientation(LinearLayout.VERTICAL);
        identity.setPadding(dp(12), 0, 0, 0);
        identity.addView(text("Signed in: " + activeMobile(), 14, DARK, true));
        identity.addView(text("NextGen mParivahan", 12, 0xFF64777D, false));
        profile.addView(identity, new LinearLayout.LayoutParams(0, -2, 1));
        drawer.addView(profile, new LinearLayout.LayoutParams(-1, dp(82)));

        ScrollView sc = new ScrollView(this);
        sc.setFillViewport(false);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(10), 0, dp(8));
        sc.addView(list);
        drawer.addView(sc, new LinearLayout.LayoutParams(-1, 0, 1));

        String[] items = {"Home","My Profile","Transport Services","My Transactions",
                "Virtual Documents","Nearby Services","Citizen Sentinel","Emergency Service",
                "Raise a Complaint","Settings","Demo People Directory","Logout"};
        int[] menuIcons = {R.drawable.ic_nav_home_blue,R.drawable.ic_nav_profile,
                R.drawable.icon_transport,R.drawable.icon_transactions,R.drawable.icon_virtual_rc,
                R.drawable.ic_nav_location_blue,R.drawable.icon_citizen,R.drawable.ic_nav_emergency,
                R.drawable.ic_nav_report,R.drawable.ic_nav_settings_blue,
                R.drawable.icon_rc_particulars,R.drawable.ic_nav_logout};

        for (int i = 0; i < items.length; i++) {
            final String item = items[i];
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(10), 0, dp(8), 0);
            row.setBackgroundResource(android.R.drawable.list_selector_background);
            row.setClickable(true);
            row.setFocusable(true);
            ImageView icon = new ImageView(this);
            icon.setImageResource(menuIcons[i]);
            icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            icon.setPadding(dp(7), dp(7), dp(7), dp(7));
            row.addView(icon, new LinearLayout.LayoutParams(dp(40), dp(48)));
            TextView label = text(item, 15, DARK, false);
            row.addView(label, new LinearLayout.LayoutParams(0, dp(48), 1));
            list.addView(row, new LinearLayout.LayoutParams(-1, dp(48)));
            row.setContentDescription(item);
            row.setOnClickListener(v -> navigateOnce(v,() -> { closeDrawer(); handleDrawer(item); }));
        }

        TextView foot = text("Staging • Version 1.7", 11, 0xFF87969B, false);
        foot.setGravity(Gravity.CENTER);
        drawer.addView(foot, new LinearLayout.LayoutParams(-1, dp(28)));
        drawer.requestFocus();
    }
    private void closeDrawer(){if(drawer!=null){root.removeView(drawer);drawer=null;}if(drawerShade!=null){root.removeView(drawerShade);drawerShade=null;}}
    private void handleDrawer(String s){switch(s){case"Home":showHome();break;case"My Profile":showProfile();break;case"Transport Services":showDataPage("Transport Services",new String[]{"Vehicle Registration","Driving Licence","Permit Services","Challan Services","Tax / Fee Information","RC Search","DL Search"});break;case"My Transactions":showTransactions();break;case"Virtual Documents":showDataPage("Virtual Documents",new String[]{"Virtual RC","Virtual DL","Shared RC","Received RC"});break;case"Nearby Services":showNearby();break;case"Citizen Sentinel":showCitizen();break;case"Emergency Service":showEmergency();break;case"Raise a Complaint":showComplaint();break;case"Settings":showSettings();break;case"Demo People Directory":showPeopleDirectory();break;case"Logout":getSharedPreferences("session",MODE_PRIVATE).edit().clear().apply();currentMobile="";navigationStack.clear();showLogin();break;}}

    private void bottomBar(LinearLayout outer){
        LinearLayout bar=new LinearLayout(this);bar.setBackground(shape(BLUE,18));bar.setGravity(Gravity.CENTER);bar.setPadding(dp(4),dp(4),dp(4),dp(4));bar.setElevation(dp(7));
        int[] icons={R.drawable.ic_nav_home,R.drawable.icon_virtual_rc,R.drawable.ic_nav_location,R.drawable.ic_nav_settings};String[] names={"Home","Documents","Nearby","Settings"};Runnable[] acts={this::showHome,()->showDataPage("Virtual Documents",new String[]{"Virtual RC","Virtual DL","Shared RC","Received RC"}),this::showNearby,this::showSettings};
        for(int i=0;i<4;i++){final int k=i;LinearLayout item=new LinearLayout(this);item.setOrientation(1);item.setGravity(Gravity.CENTER);item.setPadding(dp(2),dp(3),dp(2),dp(3));ImageView ic=new ImageView(this);ic.setImageResource(icons[i]);ic.setColorFilter(Color.WHITE);ic.setScaleType(ImageView.ScaleType.CENTER_INSIDE);ic.setPadding(dp(5),dp(2),dp(5),dp(2));item.addView(ic,new LinearLayout.LayoutParams(-1,dp(30)));TextView label=text(names[i],11,Color.WHITE,true);label.setGravity(Gravity.CENTER);label.setMaxLines(1);label.setIncludeFontPadding(true);item.addView(label,new LinearLayout.LayoutParams(-1,dp(22)));bar.addView(item,new LinearLayout.LayoutParams(0,dp(60),1));item.setOnClickListener(v->navigateOnce(v,acts[k]));item.setFocusable(true);}
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(68));bp.setMargins(dp(10),dp(3),dp(10),dp(7));outer.addView(bar,bp);
        // Every page that owns the shared bottom navigation participates in the back stack.
        navigationEnabled = true;
    }
    private void vehicleSearch(){searchDialog("Vehicle Search","Enter Vehicle No.",true);}
    private void licenceSearch(){searchDialog("Driving Licence Search","Enter DL number",false);}
    private void searchDialog(String title,String hint,boolean vehicle){EditText e=new EditText(this);e.setHint(hint);e.setSingleLine();e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});new AlertDialog.Builder(this).setTitle(title).setView(e).setNegativeButton("Cancel",null).setPositiveButton("Search",(d,w)->{String v=e.getText().toString().trim();if(v.isEmpty())toast("Enter value");else{ApiClient.recordTransaction(activeMobile(),title,v,(ok,data,error)->{if(!ok)toast("Could not save activity to API: "+error);});showSearchResult(title,v);}}).show();}
    private void showSearchResult(String title,String value){
        if(title.toLowerCase(Locale.US).contains("vehicle")){
            ApiClient.vehicle(value, (ok, data, error) -> {
                if (ok) {
                    String details = "Owner: " + data.optString("owner") + "\nState: " + data.optString("state")
                            + "\nCity: " + data.optString("city") + "\nVehicle: " + data.optString("vehicle")
                            + "\nFuel: " + data.optString("fuel") + "\nRecord: " + data.optString("status");
                    new AlertDialog.Builder(this).setTitle("Vehicle API Demo Record").setMessage(details + "\n\nFictional test data — not an official VAHAN response").setPositiveButton("OK",null).show();
                } else {
                    new AlertDialog.Builder(this).setTitle(title).setMessage("Local API request failed: " + error + "\n\nStart the Spring Boot API and try again. The app will not silently substitute phone-only data.").setPositiveButton("OK",null).show();
                }
            });
        }else{
            ApiClient.licence(value,(ok,data,error)->{if(ok)new AlertDialog.Builder(this).setTitle("Driving Licence API Demo Record").setMessage("Name: "+data.optString("owner")+"\nState: "+data.optString("state")+"\nStatus: "+data.optString("status")+"\n\nFictional local API data — not an official SARATHI response").setPositiveButton("OK",null).show();else new AlertDialog.Builder(this).setTitle(title).setMessage("No API licence record found: "+error).setPositiveButton("OK",null).show();});
        }
    }

    private void showCitizen(){
        base(); LinearLayout outer=new LinearLayout(this);outer.setOrientation(LinearLayout.VERTICAL);root.addView(outer,new FrameLayout.LayoutParams(-1,-1));
        outer.addView(subHeader("Citizen Sentinel"),new LinearLayout.LayoutParams(-1,dp(64)));
        ScrollView sc=new ScrollView(this);LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(18),dp(8),dp(18),dp(22));sc.addView(page);outer.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        TextView welcome=text("Welcome to Citizen Sentinel",22,DARK,true);welcome.setPadding(0,dp(10),0,dp(4));page.addView(welcome);
        TextView desc=text("Help make roads safer. Submit a report to the local API and explore road-safety information.",14,0xFF65747A,false);page.addView(desc);
        ImageView art=new ImageView(this);art.setImageResource(R.drawable.citizen_avatar);art.setScaleType(ImageView.ScaleType.FIT_CENTER);page.addView(art,new LinearLayout.LayoutParams(-1,dp(210)));
        featureCard(page,"Raise a Complaint","Report a traffic or vehicle-related issue", "＋",this::showComplaint);
        featureCard(page,"Road Safety Information","Guidelines, safe driving and citizen awareness", "ⓘ",()->showDataPage("Road Safety",new String[]{"Road Safety","Traffic Rules","Emergency Contacts"}));
        featureCard(page,"Citizen Alerts","Read sample road and document reminders", "♧",()->showDataPage("Citizen Alerts",new String[]{"Citizen alerts","Traffic Rules","Road Safety"}));
        featureCard(page,"Complaint Tracking","View complaints stored in the local API database", "›",this::showTransactions);
        bottomBar(outer);
    }
    private void showNearby(){
        base(); LinearLayout outer=new LinearLayout(this); outer.setOrientation(LinearLayout.VERTICAL); root.addView(outer,new FrameLayout.LayoutParams(-1,-1)); outer.addView(subHeader("Nearby Services"),new LinearLayout.LayoutParams(-1,dp(64)));
        ScrollView sc=new ScrollView(this); LinearLayout list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); list.setPadding(dp(16),dp(8),dp(16),dp(18)); sc.addView(list); outer.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        list.addView(text("Loading nearby service categories from local API...",14,DARK,false));
        ApiClient.nearby((ok,data,error)->{list.removeAllViews();if(!ok){list.addView(text("Nearby API error: "+error,14,DARK,false));return;}JSONArray rows=data.optJSONArray("items");if(rows==null||rows.length()==0){list.addView(text("No nearby categories returned by API",14,DARK,false));return;}for(int i=0;i<rows.length();i++){JSONObject r=rows.optJSONObject(i);if(r==null)continue;String id=r.optString("id"),name=r.optString("name"),detail=id.equals("pollution")?"Pollution checking centre":id.equals("rto")?"RTO":id.equals("fuel")?"Fuel pump":"Hospital";String icon=id.equals("pollution")?"▣":id.equals("rto")?"♙":id.equals("fuel")?"⛽":"✚";nearbyRow(list,name,icon,detail);}});
        bottomBar(outer);
    }
    private void nearbyRow(LinearLayout list,String title,String icon,String detail){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(4),dp(12),dp(4),dp(12));
        String asset=detail.equals("Pollution checking centre")?"nearby_pollution":detail.equals("RTO")?"nearby_rto":detail.equals("Fuel pump")?"nearby_fuel":"nearby_hospital";
        int artId=getResources().getIdentifier(asset,"drawable",getPackageName());
        if(artId!=0){ImageView art=new ImageView(this);art.setImageResource(artId);art.setScaleType(ImageView.ScaleType.FIT_CENTER);art.setBackground(stroke(0xFFD4E4EA,1,14));row.addView(art,new LinearLayout.LayoutParams(dp(94),dp(94)));}
        else {TextView art=text(icon,38,0xFF258DB5,true);art.setGravity(Gravity.CENTER);art.setBackground(stroke(0xFFD4E4EA,1,14));row.addView(art,new LinearLayout.LayoutParams(dp(94),dp(94)));}
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(14),0,0,0);TextView name=text(title,16,DARK,true);info.addView(name);TextView view=text("View Map",13,BLUE,false);view.setPadding(dp(12),dp(4),dp(12),dp(4));view.setBackground(stroke(BLUE,1,18));LinearLayout.LayoutParams vp=new LinearLayout.LayoutParams(-2,dp(32));vp.setMargins(0,dp(8),0,0);info.addView(view,vp);row.addView(info,new LinearLayout.LayoutParams(0,-2,1));list.addView(row,new LinearLayout.LayoutParams(-1,-2));View line=new View(this);line.setBackgroundColor(0xFFE7EEF1);list.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));row.setOnClickListener(v->showExpanded(detail));view.setOnClickListener(v->openNearbyMap(detail));
    }
    private void openNearbyMap(String query){
        try{Intent map=new Intent(Intent.ACTION_VIEW,Uri.parse("geo:0,0?q="+Uri.encode(query+" near me")));startActivity(map);}catch(ActivityNotFoundException ex){toast("No map application is installed on this device");}
    }
    private void showEmergency(){
        base();LinearLayout outer=new LinearLayout(this);outer.setOrientation(LinearLayout.VERTICAL);root.addView(outer,new FrameLayout.LayoutParams(-1,-1));outer.addView(subHeader("Emergency Service"),new LinearLayout.LayoutParams(-1,dp(64)));
        ScrollView sc=new ScrollView(this);LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(24),dp(12),dp(24),dp(24));page.setGravity(Gravity.CENTER_HORIZONTAL);sc.addView(page);outer.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        TextView pageTitle=text("Emergency Service",19,DARK,true);pageTitle.setGravity(Gravity.LEFT);pageTitle.setPadding(0,dp(8),0,dp(10));page.addView(pageTitle,new LinearLayout.LayoutParams(-1,dp(40)));
        ImageView art=new ImageView(this);art.setImageResource(R.drawable.emergency_cloud);art.setScaleType(ImageView.ScaleType.CENTER_INSIDE);page.addView(art,new LinearLayout.LayoutParams(-1,dp(210)));
        TextView title=text("Create Emergency Profile",18,DARK,true);title.setGravity(Gravity.CENTER);page.addView(title);TextView msg=text("For quick access during an emergency, add emergency contact",14,0xFF68757A,false);msg.setGravity(Gravity.CENTER);msg.setPadding(0,dp(8),0,dp(18));page.addView(msg);
        featureCard(page,"Manage Contact","Save an emergency contact on this device", "●",()->manageEmergencyContact());
        featureCard(page,"Call Ambulance","Get the ambulance contact from the local API", "✚",()->ApiClient.emergency((ok,data,error)->{String number="102";if(ok){JSONArray rows=data.optJSONArray("items");if(rows!=null)for(int i=0;i<rows.length();i++){JSONObject r=rows.optJSONObject(i);if(r!=null&&r.optString("name").equalsIgnoreCase("Ambulance")){number=r.optString("number","102");break;}}}final String dial=number;new AlertDialog.Builder(this).setTitle("Call ambulance?").setMessage("This will open your phone dialler for "+dial+".").setNegativeButton("Cancel",null).setPositiveButton("Continue",(d,w)->startActivity(new Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+dial)))).show();}));
        featureCard(page,"Tap To Notify Contacts","Send a notification preview to your saved contact", "♧",()->notifyEmergencyContact());
        bottomBar(outer);
    }
    private View subHeader(String title){LinearLayout h=new LinearLayout(this);h.setGravity(Gravity.CENTER_VERTICAL);h.setPadding(dp(14),0,dp(14),0);h.setBackgroundColor(Color.WHITE);h.setElevation(dp(2));TextView back=text("‹",32,0xFF315D70,false);h.addView(back,new LinearLayout.LayoutParams(dp(42),-1));back.setOnClickListener(v->navigateOnce(v,this::goBack));ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.logo);logo.setScaleType(ImageView.ScaleType.FIT_CENTER);logo.setAdjustViewBounds(true);h.addView(logo,new LinearLayout.LayoutParams(0,dp(52),1));return h;}
    private void featureCard(LinearLayout page,String title,String subtitle,String icon,Runnable action){LinearLayout card=new LinearLayout(this);card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(dp(14),dp(12),dp(14),dp(12));card.setBackground(shape(Color.WHITE,18));card.setElevation(dp(4));card.setMinimumHeight(dp(78));TextView ic=text(icon,26,0xFF263C43,true);ic.setGravity(Gravity.CENTER);ic.setBackground(stroke(0xFFE0E8EB,1,30));card.addView(ic,new LinearLayout.LayoutParams(dp(48),dp(48)));LinearLayout words=new LinearLayout(this);words.setOrientation(LinearLayout.VERTICAL);words.setPadding(dp(12),0,0,0);TextView heading=text(title,16,DARK,true);heading.setMaxLines(2);TextView detail=text(subtitle,12,0xFF6C777B,false);detail.setMaxLines(3);words.addView(heading);words.addView(detail);card.addView(words,new LinearLayout.LayoutParams(0,-2,1));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(8),0,dp(8));page.addView(card,lp);card.setOnClickListener(v->navigateOnce(v,action));}
    private void manageEmergencyContact(){EditText name=new EditText(this);name.setHint("Contact name");EditText phone=new EditText(this);phone.setHint("10 digit mobile number");phone.setInputType(3);phone.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(16),0,dp(16),0);box.addView(name);box.addView(phone);new AlertDialog.Builder(this).setTitle("Manage Emergency Contact").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{String n=name.getText().toString().trim(),p=phone.getText().toString().trim();if(n.isEmpty()||!p.matches("\\d{10}")){toast("Enter a name and valid 10 digit number");return;}getSharedPreferences("emergency",MODE_PRIVATE).edit().putString("name",n).putString("phone",p).apply();toast("Emergency contact saved");}).show();}
    private void notifyEmergencyContact(){String n=getSharedPreferences("emergency",MODE_PRIVATE).getString("name","");String p=getSharedPreferences("emergency",MODE_PRIVATE).getString("phone","");if(p.isEmpty()){toast("Please add an emergency contact first");manageEmergencyContact();return;}new AlertDialog.Builder(this).setTitle("Emergency notification preview").setMessage("Contact: "+n+" ("+p+")\nThis demo does not send an SMS automatically.").setPositiveButton("OK",null).show();}
    private void simplePage(String title,String[] items){showDataPage(title,items);}
    private void showTransactions(){
        base(); LinearLayout outer=new LinearLayout(this); outer.setOrientation(LinearLayout.VERTICAL); root.addView(outer,new FrameLayout.LayoutParams(-1,-1)); outer.addView(subHeader("My Transactions"),new LinearLayout.LayoutParams(-1,dp(64)));
        ScrollView scroll=new ScrollView(this); LinearLayout list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); list.setPadding(dp(18),dp(14),dp(18),dp(24)); scroll.addView(list); outer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView title=text("Recent API activity",19,DARK,true); title.setPadding(0,dp(4),0,dp(10)); list.addView(title);
        TextView txLoading=text("Loading transactions from local API...",14,DARK,false); list.addView(txLoading);
        TextView complaintHeading=text("Submitted complaints",19,DARK,true); complaintHeading.setPadding(0,dp(18),0,dp(8)); list.addView(complaintHeading);
        TextView cpLoading=text("Loading complaints from local API...",14,DARK,false); list.addView(cpLoading);
        ApiClient.transactions(activeMobile(),(ok,data,error)->{list.removeView(txLoading);if(!ok){list.addView(text("Transaction API error: "+error,14,DARK,false),1);return;}JSONArray rows=data.optJSONArray("items");if(rows==null||rows.length()==0){list.addView(text("No API transactions yet. Search a vehicle or licence to create one.",14,DARK,false),1);return;}for(int i=0;i<rows.length();i++){JSONObject r=rows.optJSONObject(i);if(r!=null)addHistoryCard(list,r.optString("type","Activity")+": "+r.optString("value",""),r.optString("created_at","Saved in API database"));}});
        ApiClient.complaints(activeMobile(),(ok,data,error)->{list.removeView(cpLoading);if(!ok){list.addView(text("Complaint API error: "+error,14,DARK,false));return;}JSONArray rows=data.optJSONArray("items");if(rows==null||rows.length()==0){list.addView(text("No complaints saved in the API yet.",14,DARK,false));return;}for(int i=0;i<rows.length();i++){JSONObject r=rows.optJSONObject(i);if(r!=null)addHistoryCard(list,r.optString("category")+" / "+r.optString("subcategory")+" — "+r.optString("vehicle"),r.optString("created_at","Saved in API database"));}});
        bottomBar(outer);
    }
    private void showTransactionCategory(String category){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(8),dp(4),dp(8),dp(4));TextView loading=text("Loading from API...",14,DARK,false);box.addView(loading);
        new AlertDialog.Builder(this).setTitle(category).setView(box).setPositiveButton("Close",null).show();
        ApiClient.transactions(activeMobile(),(ok,data,error)->{box.removeAllViews();if(!ok){box.addView(text("API error: "+error,14,DARK,false));return;}JSONArray rows=data.optJSONArray("items");int count=0;if(rows!=null)for(int i=0;i<rows.length();i++){JSONObject r=rows.optJSONObject(i);if(r==null)continue;String type=r.optString("type","");if(type.toLowerCase(Locale.US).contains(category.toLowerCase(Locale.US).split(" ")[0])){TextView t=text(type+": "+r.optString("value"),14,DARK,false);t.setPadding(0,dp(9),0,dp(9));box.addView(t);count++;}}if(count==0)box.addView(text("No matching API records yet.",14,DARK,false));});
    }
    private void addHistoryHeading(LinearLayout list,String title){TextView t=text(title,20,DARK,true);t.setPadding(0,dp(12),0,dp(8));list.addView(t);}
    private void addHistoryCard(LinearLayout list,String title,String detail){LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(16),dp(12),dp(16),dp(12));card.setBackground(shape(PALE,14));TextView a=text(title,16,DARK,true);TextView b=text(detail,13,Color.DKGRAY,false);card.addView(a);card.addView(b);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(8));list.addView(card,lp);}

    private void showSettings(){
        new AlertDialog.Builder(this).setTitle("Settings")
            .setItems(new String[]{"Export session/API info (JSON)","Check local API connection","Application information","Android settings"},(d,w)->{
                if(w==0) createBackup();
                else if(w==1) ApiClient.health((ok,data,error)->toast(ok?"Local API status: UP":"Local API unavailable: "+error));
                else if(w==2) toast("NextGen mParivahan • Local API connected build");
                else startActivity(new Intent(Settings.ACTION_SETTINGS));
            }).show();
    }

    private String exportSessionBackup() throws org.json.JSONException {
        JSONObject backup=new JSONObject(); backup.put("app","NextGen mParivahan"); backup.put("createdAt",System.currentTimeMillis());
        backup.put("activeMobile",activeMobile()); backup.put("apiBaseUrl",ApiClient.BASE_URL);
        backup.put("note","Business records are stored in the local Spring Boot API database. This file contains app/session metadata only.");
        return backup.toString(2);
    }

    private void createBackup(){
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "NextGen_mParivahan_Backup.json");
        try { startActivityForResult(intent, REQUEST_BACKUP); }
        catch (ActivityNotFoundException ex) { toast("No file manager is available to save the backup"); }
    }

    private void showComplaint(){
        base();LinearLayout outer=new LinearLayout(this);outer.setOrientation(LinearLayout.VERTICAL);root.addView(outer,new FrameLayout.LayoutParams(-1,-1));
        outer.addView(subHeader("Raise a complaint"),new LinearLayout.LayoutParams(-1,dp(64)));
        ScrollView scroll=new ScrollView(this);LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(22),dp(10),dp(22),dp(25));scroll.addView(page);outer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        category=text("Select Category   ▾",17,Color.WHITE,false);category.setPadding(dp(18),0,dp(18),0);category.setGravity(Gravity.CENTER_VERTICAL);category.setBackground(shape(BLUE,18));page.addView(category,new LinearLayout.LayoutParams(-1,dp(64)));category.setOnClickListener(v->chooseCategory());
        subCategory=text("Select Sub Category   ▾",17,Color.WHITE,false);subCategory.setPadding(dp(18),0,dp(18),0);subCategory.setGravity(Gravity.CENTER_VERTICAL);subCategory.setBackground(shape(BLUE,18));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(64));sp.setMargins(0,dp(14),0,0);page.addView(subCategory,sp);subCategory.setOnClickListener(v->chooseSubCategory());
        complaintVehicle=field("Enter Vehicle no.",20,false);complaintVehicle.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);complaintVehicle.setBackground(stroke(Color.GRAY,1,22));LinearLayout.LayoutParams vp=new LinearLayout.LayoutParams(-1,dp(64));vp.setMargins(0,dp(20),0,0);page.addView(complaintVehicle,vp);
        complaintRemarks=new EditText(this);complaintRemarks.setHint("Enter Remarks");complaintRemarks.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, sp(17));complaintRemarks.setGravity(Gravity.TOP);complaintRemarks.setPadding(dp(18),dp(15),dp(18),dp(10));complaintRemarks.setBackground(stroke(Color.GRAY,1,22));complaintRemarks.setFilters(new InputFilter[]{new InputFilter.LengthFilter(300)});complaintRemarks.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,dp(150));rp.setMargins(0,dp(20),0,0);page.addView(complaintRemarks,rp);
        counter=text("0/300",14,Color.RED,false);counter.setGravity(Gravity.RIGHT);page.addView(counter,new LinearLayout.LayoutParams(-1,dp(32)));complaintRemarks.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){counter.setText(s.length()+"/300");}public void afterTextChanged(Editable e){}});
        TextView attach=text("📎   Attach",17,Color.WHITE,true);attach.setGravity(Gravity.CENTER);attach.setBackground(shape(BLUE,14));LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(dp(160),dp(58));ap.gravity=Gravity.RIGHT;page.addView(attach,ap);attach.setOnClickListener(v->attachFile());
        TextView submit=text("Raise a complaint",18,Color.WHITE,true);submit.setGravity(Gravity.CENTER);submit.setBackground(shape(BLUE,16));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(64));bp.setMargins(0,dp(28),0,0);page.addView(submit,bp);submit.setOnClickListener(v->navigateOnce(v,this::submitComplaint));
        bottomBar(outer);
    }
    private void chooseCategory(){new AlertDialog.Builder(this).setTitle("Select Category").setItems(new String[]{"Traffic Violation","Accident","Vehicle Document","Driving Licence"},(d,w)->category.setText(new String[]{"Traffic Violation","Accident","Vehicle Document","Driving Licence"}[w]+"   ▾")).show();}
    private void chooseSubCategory(){new AlertDialog.Builder(this).setTitle("Select Sub Category").setItems(new String[]{"Wrong Parking","Overspeeding","Signal Violation","Other"},(d,w)->subCategory.setText(new String[]{"Wrong Parking","Overspeeding","Signal Violation","Other"}[w]+"   ▾")).show();}
    private void attachFile(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,44);}
    @Override protected void onActivityResult(int r,int c,Intent d){
        super.onActivityResult(r,c,d);
        if(c!=RESULT_OK || d==null || d.getData()==null) return;
        if(r==44){ attachment=d.getData(); toast("Attachment selected. This demo API does not upload file contents."); }
        else if(r==REQUEST_BACKUP){
            try(java.io.OutputStream out=getContentResolver().openOutputStream(d.getData())){
                if(out==null) throw new java.io.IOException("Could not open the selected file");
                out.write(exportSessionBackup().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                out.flush();
                toast("Backup saved successfully");
            }catch(Exception ex){ toast("Backup failed: "+ex.getMessage()); }
        }
    }
    private void submitComplaint(){String cat=category.getText().toString(),sub=subCategory.getText().toString(),veh=complaintVehicle.getText().toString().trim(),rem=complaintRemarks.getText().toString().trim();if(cat.startsWith("Select")||sub.startsWith("Select")){toast("Select category and sub-category");return;}if(veh.isEmpty()){toast("Enter vehicle number");return;}if(rem.isEmpty()){toast("Enter remarks");return;}String mobileNumber=activeMobile();if(!mobileNumber.matches("\\d{10}")){toast("Please sign in again");return;}ApiClient.submitComplaint(mobileNumber,cat,sub,veh,rem,(ok,data,error)->{if(ok){toast("Complaint saved by local API (demo only)");showCitizen();}else toast("Complaint was not saved: "+error);});}

    private void showProfile(){
        base(); LinearLayout outer=new LinearLayout(this); outer.setOrientation(LinearLayout.VERTICAL); root.addView(outer,new FrameLayout.LayoutParams(-1,-1));
        outer.addView(subHeader("My Profile"),new LinearLayout.LayoutParams(-1,dp(64)));
        ScrollView scroll=new ScrollView(this); LinearLayout page=new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL); page.setPadding(dp(24),dp(12),dp(24),dp(30)); scroll.addView(page); outer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        ImageView avatar=new ImageView(this); avatar.setImageResource(R.drawable.avatar); avatar.setScaleType(ImageView.ScaleType.CENTER_INSIDE); page.addView(avatar,new LinearLayout.LayoutParams(-1,dp(210)));
        TextView mobileValue=text("Loading from local API...",16,DARK,false), nameValue=text("",16,DARK,false), emailValue=text("",16,DARK,false);
        addInfo(page,"Mobile No.",mobileValue); addInfo(page,"Name",nameValue); addInfo(page,"Email id",emailValue);
        TextView edit=text("Edit Profile",16,Color.WHITE,true); edit.setGravity(Gravity.CENTER); edit.setBackground(shape(BLUE,14)); LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,dp(54)); ep.setMargins(0,dp(16),0,0); page.addView(edit,ep);
        edit.setOnClickListener(v->editProfileFromApi(nameValue.getText().toString(),emailValue.getText().toString()));
        bottomBar(outer);
        ApiClient.profile(activeMobile(),(ok,data,error)->{if(ok){mobileValue.setText(data.optString("mobile",activeMobile()));nameValue.setText(data.optString("name",""));emailValue.setText(data.optString("email",""));}else mobileValue.setText("API error: "+error);});
    }
    private void editProfileFromApi(String name,String email){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(18),0,dp(18),0);
        EditText n=new EditText(this); n.setText(name); n.setHint("Name"); EditText e=new EditText(this); e.setText(email); e.setHint("Email"); box.addView(n); box.addView(e);
        new AlertDialog.Builder(this).setTitle("Edit Profile").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->ApiClient.updateProfile(activeMobile(),n.getText().toString(),e.getText().toString(),(ok,data,error)->{if(ok){toast("Profile updated in local API");showProfile();}else toast("Profile update failed: "+error);})).show();
    }
    private void addInfo(LinearLayout p,String label,String value){TextView v=text(value,17,DARK,false);addInfo(p,label,v);}
    private void addInfo(LinearLayout p,String label,TextView v){TextView l=text(label,17,Color.BLACK,true);l.setPadding(0,dp(18),0,dp(6));p.addView(l);v.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,sp(17));v.setTextColor(DARK);v.setPadding(dp(16),0,dp(16),0);v.setGravity(Gravity.CENTER_VERTICAL);v.setBackground(shape(PALE,16));p.addView(v,new LinearLayout.LayoutParams(-1,dp(64)));}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}

    @Override protected void onDestroy(){
        handler.removeCallbacksAndMessages(null);
        if (Build.VERSION.SDK_INT >= 33 && systemBackCallback != null) {
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback((android.window.OnBackInvokedCallback) systemBackCallback);
            systemBackCallback = null;
        }
        super.onDestroy();
    }

    private void goBack() {
        if (drawer != null) { closeDrawer(); return; }
        if (!navigationStack.isEmpty()) {
            ScreenSnapshot previous = navigationStack.pop();
            root = previous.view;
            homeVisible = previous.wasHome;
            root.setAlpha(0f);
            setContentView(root);
            root.animate().alpha(1f).setDuration(170).start();
            drawer = null;
            drawerShade = null;
            return;
        }
        if (homeVisible) {
            new AlertDialog.Builder(this)
                .setTitle("Exit NextGen mParivahan?")
                .setMessage("Do you want to close the application?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Exit", (dialog, which) -> finish())
                .show();
        } else {
            showHome();
        }
    }

    @Override public void onBackPressed(){
        goBack();
    }

}

