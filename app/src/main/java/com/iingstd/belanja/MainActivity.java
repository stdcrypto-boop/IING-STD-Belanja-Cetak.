package com.iingstd.belanja;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import android.text.InputType;
import org.json.*;
import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private final ArrayList<Item> items = new ArrayList<>();
    private LinearLayout root, list;
    private TextView totalLabel, printerLabel;
    private String printerMac = "";
    private final int green = Color.rgb(19, 185, 130), bg = Color.rgb(13, 20, 27), panel = Color.rgb(27, 39, 48);
    private static final UUID SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    static class Item {
        String name, qty, unit, price;
        boolean done;
        JSONObject json() throws JSONException { JSONObject j = new JSONObject(); j.put("name",name);j.put("qty",qty);j.put("unit",unit);j.put("price",price);j.put("done",done);return j; }
        static Item from(JSONObject j){Item i=new Item();i.name=j.optString("name");i.qty=j.optString("qty","1");i.unit=j.optString("unit","pcs");i.price=j.optString("price","");i.done=j.optBoolean("done");return i;}
    }
    @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);load();render();}
    private int dp(int n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
    private GradientDrawable shape(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(16));return d;}
    private TextView text(String value,int size,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(Color.WHITE);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout.LayoutParams lp(int w,int h,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));p.topMargin=dp(top);return p;}
    private Button button(String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setBackground(shape(green));return b;}
    private void render(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(bg);
        root=column();root.setPadding(dp(18),dp(22),dp(18),dp(30));scroll.addView(root);setContentView(scroll);
        TextView title=text("STD Belanja",30,true);title.setTextColor(green);root.addView(title);
        root.addView(text("BELANJA  •  CETAK STRUK",13,true),lp(-1,-2,3));
        TextView subtitle=text("Catat belanja, simpan offline, cetak Bluetooth 58 mm",13,false);subtitle.setTextColor(0xffb6c5ce);root.addView(subtitle,lp(-1,-2,12));
        Button add=button("+ Tambah barang");root.addView(add,lp(-1,54,22));add.setOnClickListener(v->editItem(-1));
        Button bulk=button("+ Tambah banyak barang sekaligus");bulk.setBackground(shape(panel));root.addView(bulk,lp(-1,54,10));bulk.setOnClickListener(v->addManyItems());
        TextView heading=text("DAFTAR BELANJA",17,true);root.addView(heading,lp(-1,-2,24));
        list=column();root.addView(list,lp(-1,-2,10));
        totalLabel=text("",15,true);totalLabel.setTextColor(green);root.addView(totalLabel,lp(-1,-2,15));
        printerLabel=text("Printer: "+(printerMac.isEmpty()?"belum dipilih":getPreferences(0).getString("printerName",printerMac)),14,false);root.addView(printerLabel,lp(-1,-2,25));
        Button select=button("Pilih printer Bluetooth");root.addView(select,lp(-1,50,10));select.setOnClickListener(v->choosePrinter());
        Button preview=button("Lihat pratinjau struk");preview.setBackground(shape(panel));root.addView(preview,lp(-1,50,12));preview.setOnClickListener(v->previewReceipt());
        Button print=button("CETAK DAFTAR BELANJA");root.addView(print,lp(-1,56,12));print.setOnClickListener(v->printReceipt());
        Button clear=button("Hapus semua barang");clear.setBackground(shape(panel));root.addView(clear,lp(-1,50,12));clear.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Hapus daftar?").setMessage("Semua barang pada daftar ini akan dihapus.").setNegativeButton("Batal",null).setPositiveButton("Hapus",(d,w)->{items.clear();save();refresh();}).show());
        refresh();
    }
    private void refresh(){
        list.removeAllViews();double sum=0;int checked=0;
        for(int idx=0;idx<items.size();idx++){
            final int pos=idx;Item item=items.get(idx);
            LinearLayout card=column();card.setPadding(dp(14),dp(10),dp(14),dp(12));card.setBackground(shape(panel));
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
            CheckBox check=new CheckBox(this);check.setButtonTintList(android.content.res.ColorStateList.valueOf(green));check.setChecked(item.done);row.addView(check);check.setOnCheckedChangeListener((b,c)->{item.done=c;save();refresh();});
            LinearLayout info=column();TextView name=text(item.name,17,true);info.addView(name);TextView detail=text(item.qty+" "+item.unit+(item.price.isEmpty()?"":"  •  Rp "+item.price),13,false);detail.setTextColor(0xffb9c7cd);info.addView(detail);row.addView(info,new LinearLayout.LayoutParams(0,-2,1));
            Button edit=button("Ubah");edit.setTextSize(12);row.addView(edit,new LinearLayout.LayoutParams(dp(72),dp(44)));edit.setOnClickListener(v->editItem(pos));
            card.addView(row);list.addView(card,lp(-1,-2,8));
            if(item.done)checked++;try{sum+=Double.parseDouble(item.qty.replace(',','.'))*Double.parseDouble(item.price.replace(".","").replace(',','.'));}catch(Exception ignored){}
        }
        if(items.isEmpty()){TextView empty=text("Belum ada barang. Tekan + Tambah barang.",14,false);empty.setTextColor(0xffb9c7cd);list.addView(empty,lp(-1,-2,12));}
        totalLabel.setText(items.size()+" barang  •  "+checked+" selesai"+(sum>0?"  •  Estimasi Rp "+String.format(Locale.US,"%,.0f",sum):""));
    }
    private EditText field(String hint,String value){EditText e=new EditText(this);e.setSingleLine(true);e.setHint(hint);e.setText(value);e.setTextColor(Color.BLACK);e.setHintTextColor(0xff777777);return e;}
    private void editItem(int position){
        Item old=position<0?null:items.get(position);
        LinearLayout form=column();form.setPadding(dp(20),dp(8),dp(20),0);
        EditText name=field("Nama barang, misalnya telur",old==null?"":old.name);
        EditText qty=field("Jumlah, misalnya 2",old==null?"1":old.qty);qty.setInputType(InputType.TYPE_CLASS_TEXT);
        EditText unit=field("Satuan, misalnya kg / pcs / liter",old==null?"pcs":old.unit);
        EditText price=field("Harga per satuan (opsional)",old==null?"":old.price);price.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        form.addView(name);form.addView(qty);form.addView(unit);form.addView(price);
        AlertDialog.Builder builder=new AlertDialog.Builder(this).setTitle(position<0?"Tambah barang":"Ubah barang").setView(form).setNegativeButton("Batal",null).setPositiveButton("Simpan",null);
        if(position>=0)builder.setNeutralButton("Hapus",(d,w)->{items.remove(position);save();refresh();});
        AlertDialog dialog=builder.create();dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String n=name.getText().toString().trim(),q=qty.getText().toString().trim(),u=unit.getText().toString().trim();
            if(n.isEmpty()||q.isEmpty()){Toast.makeText(this,"Isi nama dan jumlah barang",Toast.LENGTH_SHORT).show();return;}
            Item item=old==null?new Item():old;item.name=n;item.qty=q;item.unit=u.isEmpty()?"pcs":u;item.price=price.getText().toString().trim();if(old==null)items.add(item);save();refresh();dialog.dismiss();
        }));dialog.show();
    }
    private void addManyItems(){
        LinearLayout form=column();
        form.setPadding(dp(20),dp(12),dp(20),0);
        TextView help=text("Satu barang per baris: nama, jumlah, satuan\\nContoh:\\nBeras, 2, kg\\nMinyak sayur, 1, liter\\nTelur, 0.5, kg",14,false);
        help.setTextColor(Color.DKGRAY);
        form.addView(help);
        EditText lines=new EditText(this);
        lines.setSingleLine(false);
        lines.setMinLines(6);
        lines.setGravity(Gravity.TOP);
        lines.setTextColor(Color.BLACK);
        lines.setHint("Beras, 2, kg\\nMinyak sayur, 1, liter\\nTelur, 0.5, kg");
        lines.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        form.addView(lines);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Tambah banyak barang")
            .setView(form).setNegativeButton("Batal",null)
            .setPositiveButton("Simpan semua",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String raw=lines.getText().toString().trim();
            if(raw.isEmpty()){Toast.makeText(this,"Tulis daftar barang dahulu",Toast.LENGTH_SHORT).show();return;}
            String[] rows=raw.split("\\r?\\n");
            ArrayList<Item> parsed=new ArrayList<>();
            for(int k=0;k<rows.length;k++){
                String row=rows[k].trim();
                if(row.isEmpty())continue;
                String[] parts=row.split(",",-1);
                if(parts.length<2||parts.length>3||parts[0].trim().isEmpty()||parts[1].trim().isEmpty()){
                    message("Periksa baris "+(k+1)+": "+row+"\\nFormat: nama, jumlah, satuan");return;
                }
                Item item=new Item();
                item.name=parts[0].trim();item.qty=parts[1].trim();
                item.unit=parts.length==3&&!parts[2].trim().isEmpty()?parts[2].trim():"pcs";
                item.price="";item.done=false;parsed.add(item);
            }
            if(parsed.isEmpty()){message("Belum ada barang yang bisa disimpan.");return;}
            items.addAll(parsed);save();refresh();dialog.dismiss();
            Toast.makeText(this,parsed.size()+" barang berhasil ditambahkan",Toast.LENGTH_LONG).show();
        }));
        dialog.show();
    }
    private void load(){printerMac=getPreferences(0).getString("printerMac","");try{JSONArray a=new JSONArray(getPreferences(0).getString("items","[]"));for(int k=0;k<a.length();k++)items.add(Item.from(a.getJSONObject(k)));}catch(Exception ignored){}}
    private void save(){JSONArray a=new JSONArray();for(Item i:items){try{a.put(i.json());}catch(Exception ignored){}}getPreferences(0).edit().putString("items",a.toString()).apply();}
    private boolean bluetoothAllowed(){
        if(Build.VERSION.SDK_INT>=31&&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},20);Toast.makeText(this,"Izinkan Bluetooth, lalu coba lagi",Toast.LENGTH_LONG).show();return false;}return true;
    }
    private void choosePrinter(){
        if(!bluetoothAllowed())return;
        BluetoothManager manager=(BluetoothManager)getSystemService(BLUETOOTH_SERVICE);BluetoothAdapter adapter=manager==null?null:manager.getAdapter();
        if(adapter==null){message("Bluetooth tidak tersedia di HP ini.");return;}
        if(!adapter.isEnabled()){message("Aktifkan Bluetooth dan pasangkan printer lewat Pengaturan HP dahulu.");return;}
        Set<BluetoothDevice> paired;
        try{paired=adapter.getBondedDevices();}catch(SecurityException e){message("Izin Bluetooth diperlukan.");return;}
        if(paired.isEmpty()){message("Belum ada perangkat yang dipasangkan. Pair printer di Pengaturan Bluetooth HP dahulu.");return;}
        ArrayList<BluetoothDevice> devices=new ArrayList<>(paired);devices.sort((a,b)->String.valueOf(a.getName()).compareToIgnoreCase(String.valueOf(b.getName())));
        String[] names=new String[devices.size()];for(int k=0;k<devices.size();k++)names[k]=String.valueOf(devices.get(k).getName())+" ("+devices.get(k).getAddress()+")";
        new AlertDialog.Builder(this).setTitle("Pilih printer yang sudah dipasangkan").setItems(names,(d,which)->{
            BluetoothDevice selected=devices.get(which);printerMac=selected.getAddress();getPreferences(0).edit().putString("printerMac",printerMac).putString("printerName",String.valueOf(selected.getName())).apply();printerLabel.setText("Printer: "+selected.getName());
        }).setNegativeButton("Batal",null).show();
    }
    private void previewReceipt(){
        if(items.isEmpty()){message("Daftar belanja masih kosong.");return;}
        StringBuilder sb=new StringBuilder("STD\\nDAFTAR BELANJA\\n\\n");
        for(int k=0;k<items.size();k++){
            Item i=items.get(k);
            sb.append(k+1).append(". ").append(i.name).append("\\n");
            sb.append("   ").append(i.qty).append(" ").append(i.unit).append("\\n");
            sb.append("   Harga: Rp __________\\n\\n");
        }
        sb.append("------------------------------\\nTOTAL: Rp ______________\\nJANGAN ADA YANG LUPA!");
        new AlertDialog.Builder(this).setTitle("Pratinjau daftar cetak").setMessage(sb.toString()).setNegativeButton("Tutup",null).setPositiveButton("Cetak",(d,w)->printReceipt()).show();
    }
    private void printReceipt(){
        if(items.isEmpty()){message("Tambahkan barang dahulu.");return;}
        if(printerMac.isEmpty()){choosePrinter();return;}
        if(!bluetoothAllowed())return;
        BluetoothManager manager=(BluetoothManager)getSystemService(BLUETOOTH_SERVICE);BluetoothAdapter adapter=manager==null?null:manager.getAdapter();
        if(adapter==null||!adapter.isEnabled()){message("Aktifkan Bluetooth dahulu.");return;}
        ProgressDialog progress=new ProgressDialog(this);progress.setMessage("Menghubungkan dan mencetak...");progress.setCancelable(false);progress.show();
        final String mac=printerMac;final byte[] bytes=receipt();
        new Thread(()->{
            BluetoothSocket socket=null;String error=null;
            try{
                BluetoothDevice device=adapter.getRemoteDevice(mac);
                socket=device.createRfcommSocketToServiceRecord(SPP);
                // Printer is already paired; avoid cancelDiscovery(), which requires BLUETOOTH_SCAN on Android 12+.
                socket.connect();
                OutputStream out=socket.getOutputStream();out.write(bytes);out.flush();Thread.sleep(500);
            }catch(Exception e){error=e.getClass().getSimpleName()+": "+e.getMessage();}
            finally{if(socket!=null)try{socket.close();}catch(IOException ignored){}}
            final String result=error;runOnUiThread(()->{if(!isFinishing())progress.dismiss();message(result==null?"Data cetak terkirim ke printer. Periksa kertas hasil cetak.":"Gagal mencetak: "+result+". Pastikan printer menyala dan tidak dipakai aplikasi lain.");});
        }).start();
    }
    private byte[] receipt(){
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try{
            out.write(new byte[]{0x1b,0x40}); // Inisialisasi ESC/POS
            out.write(new byte[]{0x1b,0x61,0x01}); // Rata tengah
            out.write(new byte[]{0x1b,0x45,0x01}); // Tebal
            out.write(new byte[]{0x1d,0x21,0x11}); // Judul 2x
            write(out,"STD\\n");
            out.write(new byte[]{0x1d,0x21,0x00});
            write(out,"DAFTAR BELANJA\\n");
            out.write(new byte[]{0x1b,0x45,0x00});
            write(out,new SimpleDateFormat("dd-MM-yyyy HH:mm",new Locale("id","ID")).format(new Date())+"\\n");
            write(out,"--------------------------------\\n");
            out.write(new byte[]{0x1b,0x61,0x00}); // Kiri
            for(int k=0;k<items.size();k++){
                Item i=items.get(k);
                out.write(new byte[]{0x1b,0x45,0x01});
                // Normal-width bold fits more text on 58mm than double-width.
                String name=(k+1)+". "+safe(i.name).toUpperCase(new Locale("id","ID"));
                for(String line:wrapReceipt(name,32))write(out,line+"\\n");
                String qty="   "+safe(i.qty)+" "+safe(i.unit);
                for(String line:wrapReceipt(qty,32))write(out,line+"\\n");
                out.write(new byte[]{0x1b,0x45,0x00});
                write(out,"   Harga: Rp ______________\\n");
                if(k<items.size()-1)write(out,"--------------------------------\\n");
            }
            write(out,"--------------------------------\\n");
            out.write(new byte[]{0x1b,0x45,0x01});
            write(out,"TOTAL: Rp ____________________\\n");
            out.write(new byte[]{0x1b,0x61,0x01});
            write(out,"JANGAN ADA YANG LUPA!\\n");
            out.write(new byte[]{0x1b,0x45,0x00});
            write(out,"\\n\\n\\n");
        }catch(IOException ignored){}
        return out.toByteArray();
    }
    private ArrayList<String> wrapReceipt(String value,int max){
        ArrayList<String> lines=new ArrayList<>();
        String remaining=value.trim();
        while(remaining.length()>max){
            int split=remaining.lastIndexOf(' ',max);
            if(split<=0)split=max;
            lines.add(remaining.substring(0,split).trim());
            remaining=remaining.substring(split).trim();
        }
        if(!remaining.isEmpty())lines.add(remaining);
        return lines;
    }
    private String safe(String s){return s.replaceAll("[\\r\\n\\t]"," ");}
    private void write(ByteArrayOutputStream out,String s)throws IOException{out.write(s.getBytes(Charset.forName("CP437")));}
    private void message(String msg){new AlertDialog.Builder(this).setMessage(msg).setPositiveButton("OK",null).show();}
}
