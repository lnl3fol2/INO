/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ino;

import java.io.File;
import java.io.IOException;
import static java.lang.Math.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.util.Date;
import java.util.Scanner;
import javax.swing.JFileChooser;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JFileChooser.CANCEL_OPTION;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.apache.log4j.BasicConfigurator;

/**
 * @author Maoufumi  
 */
public class INO {

    /**
     * @param args the command line arguments
     */
    static int w1;
    static int h1;
    static int w2;
    static int h2;
    static int maxw;
    static int maxh;
    static FFmpeg ffmpeg;
    static FFprobe ffprobe;
    static BigDecimal ipphoneUser = new BigDecimal("204.09572");
    static BigDecimal ipphoneCall = new BigDecimal("203.65468");
    static BigDecimal videoUser = new BigDecimal("146.46414");
    static BigDecimal videoCall = new BigDecimal("147.52539");    
    static BigDecimal sixty = new BigDecimal("60");    
    
    public static void main(String[] args) throws IOException {
        BasicConfigurator.configure();
        ffmpeg = new FFmpeg("C:\\Users\\maoufumi\\Downloads\\ffmpeg\\bin\\ffmpeg.exe");                     //path to ffmpeg.exe
        ffprobe = new FFprobe("C:\\Users\\maoufumi\\Downloads\\ffmpeg\\bin\\ffprobe.exe");                  //path to ffprobe.exe
        String out;        
        delete("v1.mp4");
        delete("v2.mp4");
        delete("v1p.mp4");
        delete("v2p.mp4");
        delete("vh.mp4");
        delete("s1.wav");
        delete("s2.wav");
        delete("spad.wav");
        delete("smix.wav");
        delete("fin.mp4");
        
        //video processing start here        
        File vidUser = getFile();
        File vidCall = getFile();       
        
        out = "v1.mp4";
        vidUser = build(vidUser,out);
        boolean open1 = check(vidUser);
        
        out = "v2.mp4";
        vidCall = build(vidCall,out);
        boolean open2 = check(vidCall);
        
        FFmpegProbeResult res1 = null;
        FFmpegProbeResult res2 = null;
        
        if(open1){
            res1 = ffprobe.probe(vidUser.getAbsolutePath());
            if(open2){                
                res2 = ffprobe.probe(vidCall.getAbsolutePath());
                setData(res1, res2);
                out = "v1p.mp4";
                vidUser = pad(vidUser,out);
                open1 = check(vidUser);
                out = "v2p.mp4";
                vidCall = pad(vidCall,out);
                open2 = check(vidCall);
            }else{
                setData(res1, res2);
                out = "v1p.mp4";
                vidUser = pad(vidUser,out);
                open1 = check(vidUser);
            }
        }else{
            if(open2){            
                res2 = ffprobe.probe(vidCall.getAbsolutePath());
                setData(res1, res2);
                out = "v2p.mp4";
                vidCall = pad(vidCall,out);
                open2 = check(vidCall);
            }else{
                System.exit(-1);
            }
        }        
        File output = null;
        if(!open1&&open2){// 1fail
            out = "vh.mp4";
            output = padDark(vidCall,out);
            check(output);
        }else if(open1&&!open2){//2 fail
            out = "vh.mp4";
            output = padDark(vidUser,out);
            check(output);
        }else if(open1&&open2){ 
            out = "vh.mp4";
            output = hstack(vidUser,vidCall,out); 
            check(output);
        }else{//both fail
            System.exit(-1);
        }
        
        //audio processing start here
        String str = setDelta(ipphoneUser,ipphoneCall);        
        File phoneCall = getFile();        
        File phoneUser = getFile();            
        boolean open3;
        boolean open4;
        
        out = "s1.wav";
        phoneCall = buildS(phoneCall,out);
        open3 = check(phoneCall);
        
        out = "s2.wav";
        phoneUser = buildS(phoneUser,out);
        open4 = check(phoneUser);
        
        out = "spad.wav";
        if(ipphoneUser.compareTo(ipphoneCall)>0){
            if(open4){
                phoneUser = padS(phoneUser,str,out);
            }
        }else{            
            if(open3){
                phoneCall = padS(phoneCall,str,out);
            }
        }
        
        open3 = check(phoneCall);
        open4 = check(phoneUser);
        
        File soutput = null;
        if(open3&&open4){ 
            out = "smix.wav";
            soutput = mix(phoneCall,phoneUser,out);
            check(soutput);
        }else if(open3&&!open4){//4 fail
            soutput = phoneCall;
        }else if(!open3&&open4){//3 fail
            soutput = phoneUser;
        }else{//both fail
            
        }  
        
        //combination of video and audio start here
        if(check(output)&&check(soutput)){
            out = "fin.mp4";
            File fin = combine(output,soutput,out);
            System.out.println(check(fin));
        } 
    }    
    private static File getFile(){                                              //file selector
        System.err.println("getFile");
        JFileChooser fc = new JFileChooser("."); //default file location        
        fc.setDialogTitle("select file");        
        int sel = fc.showDialog(fc, "select");
        File file = null;
        switch (sel) {                                                          //check to make sure that a file is selected
            case APPROVE_OPTION:
                file = fc.getSelectedFile();
                System.out.println("you selected: "+file.getName());
                break;
            case CANCEL_OPTION:
                System.err.println("Canceled!");
                System.exit(0);
            default:
                System.err.println("An error has occurred");
                System.exit(-1);
        }                
        return file;
    }    
    private static String getOut(){                                             //get output file directory and name
        System.err.println("getOut");
        Scanner sc = new Scanner(System.in);
        Date date = new Date();
        String output = date.toString().replace(' ', '-');
        //System.out.println("Date: "+output);
        System.out.print("please name output file: ");        
        output = sc.nextLine();                                                 //scanner get output file name
        System.out.print("please select output directory: ");        
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File("C:\\Users\\58336322\\Downloads\\fileTest\\rawData\\1")); //default file location
        chooser.setDialogTitle("select directory");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);        
        int sel = chooser.showDialog(chooser, "select");
        String dir = chooser.getSelectedFile().getAbsolutePath();
        switch (sel) {                                                          //check to make sure that a file is selected
            case APPROVE_OPTION:                
                System.out.println("you selected: "+dir);
                break;
        case CANCEL_OPTION:
                System.err.println("Canceled!");
                System.exit(0);
        default:
                System.err.println("An error has occurred");
                System.exit(-1);
        }
        String out = dir+"\\"+output;                                           //concat output directory and output file name
        //System.out.println("Your selected directory: "+out);
        return out;
    }
    private static File build(File file,String out){                            //create video file from input
        // C:\FFmpeg\bin\ffmpeg.exe -y -v quiet -i input -filter_complex "setpts=PTS*0.25" [-f mp4 -vcodec libx264 -pix_fmt yuv420p -r 25/1] -an -sn output
        System.err.println("build");
        FFmpegBuilder builder = new FFmpegBuilder()
                .overrideOutputFiles(true)                                      //ffmpeg                
                .addInput(file.getAbsolutePath())                               //-i input
                .setComplexFilter("setpts=PTS*0.25")                            //-filter_complex "setpts=PTS*0.25
                
                .addOutput(out)                
                //.setFormat("mp4")                                               //-f mp4
                //.setVideoCodec("libx264")                                       //-vcodec libx264
                //.setVideoPixelFormat("yuv420p")                                 //-pix_fmt yuv420p
                //.setVideoFrameRate(25, 1)                                       //-r 25/1
                
                .disableAudio()                                                 //-an
                .disableSubtitle()                                              //-sn
                .done();                                                        //output
        builder.setVerbosity(FFmpegBuilder.Verbosity.QUIET);                    //-v quitet //set ffmpeg verbosity to not return any dialog
        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        FFmpegJob job = executor.createJob(builder);
        try{
            job.run();
        }catch(Exception E){
            System.err.println(E.getMessage());
        }
        File fileout = new File(out);
        return fileout;
    }
    private static boolean check(File file){                                    //check if file can be openned and played
        System.err.println("check");
        boolean b = false;        
        if(file.isFile()){                                                      //file exist and is a file
            try{
                FFmpegProbeResult resout = ffprobe.probe(file.getAbsolutePath());//probe
                if(
                    //resout.getFormat().bit_rate>resout.getFormat().size || 
                    resout.getFormat().bit_rate>5000000){                       //random condition
                    b = false;
                }else{                    
                    b = true;
                } 
            }catch(Exception e){                                                //probe return error
                System.err.println(e.getMessage());                
                b = false;
            }                               
        }else{            
            b = false;
        }
        System.out.println(b);
        return b;
    }
    private static void setData(FFmpegProbeResult r1,FFmpegProbeResult r2){     //set maxWidth and maxHeight
        System.err.println("set data");
        if(r1!=null){                                                           //if file1 is not null
            w1 = r1.getStreams().get(0).width;
            h1 = r1.getStreams().get(0).height;
        }        
        if(r2!=null){                                                           //if file2 is not null
            w2 = r2.getStreams().get(0).width;
            h2 = r2.getStreams().get(0).height;
        }
        if(r1!=null&&r2!=null){                                                 //if both files are not null
            maxw = max(w1,w2);
            maxh = max(h1,h2);
        }else if(r1==null&&r2!=null){                                           //if file2 is null
            maxw = w2;
            maxh = h2;
        }else if(r1!=null&&r2==null){                                           //if file1 is null
            maxw = w1;
            maxh = h1;
        }else{                                                                  //if both files are null
            System.exit(-1);
        }
        System.out.println(maxw+" "+maxh);
    }
    private static File pad(File file,String out) throws IOException{           //pad video to have same size
        // ffmpeg -y -v error -i input -filter_complex "pad=maxW:maxH:offsetW:offsetH" output
        System.err.println("pad");
        FFmpegProbeResult res = ffprobe.probe(file.getAbsolutePath());        
        FFmpegBuilder builder = new FFmpegBuilder()
                .overrideOutputFiles(true)                                      //-y
                .addInput(file.getAbsolutePath())                               //-i input                
                .setComplexFilter("pad="+maxw+":"+maxh+":"                      //-filter_complex pad=maxW:maxH:offsetW:offsetH
                        +((maxw-res.getStreams().get(0).width)/2)+":"+((maxh-res.getStreams().get(0).height)/2))
                .addOutput(out)                                                 //output
                .done();
        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        FFmpegJob job = executor.createJob(builder);
        try{
            job.run();
        }catch(Exception E){
            System.err.println(E.getMessage());
        }
        File fileout = new File(out);
        return fileout;
    }
    private static File padDark(File file,String out) throws IOException{       //pad video with black screen in case of 1 vid error
        // ffmpeg -y -v error -i input -filter_complex "pad=maxW*2:maxh" output
        System.err.println("padDark");               
        FFmpegBuilder builder = new FFmpegBuilder()
                .overrideOutputFiles(true)                                      //-y
                .addInput(file.getAbsolutePath())                               //-i input                
                .setComplexFilter("pad="+maxw*2+":"+maxh)                       //-filter_complex pad=maxW*2:maxH
                .addOutput(out)                                                 //output
                .done();
        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        FFmpegJob job = executor.createJob(builder);
        try{
            job.run();
        }catch(Exception E){
            System.err.println(E.getMessage());
        }
        File fileout = new File(out);
        return fileout;
    }
    private static File hstack(File f1, File f2, String out) throws IOException{//place both video side by side
        // ffmpeg -y -v error -i input1 -i input2 -filter_complex "hstack" output
        System.err.println("hstack");
        FFmpegBuilder builder = new FFmpegBuilder()                
                .overrideOutputFiles(true)                                      //-y
                .addInput(f1.getAbsolutePath())                                 //-i input1
                .addInput(f2.getAbsolutePath())                                 //-i input2         
                .setComplexFilter("[1][0]hstack")                               //-filter_complex "hstack"
                .addOutput(out)                                                 //output                
                .done();
        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        FFmpegJob job = executor.createJob(builder);        
        try{
            job.run();
        }catch(Exception E){
            System.err.println(E.getMessage());
        }
        File fileout = new File(out);
        return fileout;
    }
    private static String setDelta(BigDecimal bd1,BigDecimal bd2) {             //set time difference between 2 sound inputs
        System.err.println("set delta");
        BigDecimal locald = bd1.subtract(bd2).abs();                            // diff = |timeUser - timeCall| = 0.44104
        //System.out.println("diff "+locald);
        BigDecimal localdx = locald.divide(sixty, RoundingMode.HALF_UP);        // dx = diff/60 = 0.00735
        //System.out.println("diffx "+localdx);        
        BigDecimal localmin = localdx.setScale(0, RoundingMode.DOWN);           // min = first digit of dx = 0
        //System.out.println("min "+localmin);
        BigDecimal localsec = localdx.multiply(sixty);                          // sec = dx*60 = 0.44100
        //System.out.println("sec1 "+localsec);
        BigDecimal localpoint = localsec.setScale(2, RoundingMode.HALF_UP);     // point = first 3 digit of sec
        localpoint = localpoint.subtract(localpoint.setScale(0, RoundingMode.DOWN)).scaleByPowerOfTen(2);//point = xyz - x00 = yz (drop first digit) = 44
        localsec = localsec.setScale(0, RoundingMode.DOWN);                     // sec = first digit of sec = 0
        //System.out.println("sec2 "+localsec);                     
        //System.out.println("point "+localpoint);
        String str = String.format("%02d:%02d.%02d", localmin.intValue(),localsec.intValue(),localpoint.intValue());//format into %2d:%2d.%2d
        System.out.println(str);
        return str;
    }
    private static File buildS(File file,String out) {                          //create .wav file from input
        // ffmpeg -y -v error -f mulaw -ar 8000 -i input output
        System.err.println("buildS");        
        FFmpegBuilder builder;
        
        builder = new FFmpegBuilder()
                .overrideOutputFiles(true)                                      //-y
                .setFormat("mulaw")                                             //-f mulaw
                .addExtraArgs("-ar","8000")                                     //-ar 8000
                .addInput(file.getAbsolutePath())                               //-i input
                
                .addOutput(out)                                                 //output
                //.setAudioCodec("copy")                
                .done();        
        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        FFmpegJob job = executor.createJob(builder);
        try{
            job.run();
        }catch(Exception E){
            System.err.println(E.getMessage());
        }
        File fileout = new File(out);
        return fileout;
    }
    private static File padS(File file, String string, String out) {            //pad sound with higher time with silence
        System.err.println("padS");
        // ffmpeg -y -v error -t 00:00.44 -i silence.avi -i input -filter_complex concat=n=2:v=0:a=1 output
        FFmpegBuilder builder = new FFmpegBuilder()       
                .overrideOutputFiles(true)                                      //-y
                .addExtraArgs("-t",string)                                      //-t 00:00.44 //read silence.avi for duration
                .addInput("silence.avi")                                        //-i silence.avi
                .addInput(file.getAbsolutePath())                               //-i input
                .setComplexFilter("concat=n=2:v=0:a=1")                         //-filter_complex concat=n=2:v=0:a=1
                .addOutput(out)                                                 //output
                
                .done();
        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        FFmpegJob job = executor.createJob(builder);
        try{
            job.run();
        }catch(Exception E){
            System.err.println(E.getMessage());
        }
        File fileout = new File(out);
        return fileout;
    }
    private static File mix(File f1, File f2, String out) throws IOException {  //mix 2 sound input into one
        // ffmpeg -y -v error -i input1 -i input2 -filter_complex "amix" output
        System.err.println("mix");
        FFmpegBuilder builder = new FFmpegBuilder()                        
                .overrideOutputFiles(true)                                      //-y
                .addInput(f1.getAbsolutePath())                                 //-i input1
                .addInput(f2.getAbsolutePath())                                 //-i input2
                .setComplexFilter("amix")                                       //-filter_complex smix
                .addOutput(out)                                                 //output
                
                .done();
        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        FFmpegJob job = executor.createJob(builder);
        try{
            job.run();
        }catch(Exception E){
            System.err.println(E.getMessage());
        }
        File fileout = new File(out);
        return fileout;
    }
    private static File combine(File f1, File f2, String out) throws IOException {//combine stacked video with mixed sound into complete file        
        System.err.println("combine");
        BigDecimal audit = ipphoneUser.min(ipphoneCall);
        BigDecimal vdot = videoUser.min(videoCall);
        String delta = setDelta(audit,vdot);                                    //delta = difference of arrive time of inputV and inputS
        FFmpegBuilder builder;
        //ffmpeg -t delta -i silence -i inputV -i inputS -filter_complex "[0:a][2:a]concat=n=2:v=0:a=1[a]" -map 1 -map "[a]" output
        if(audit.compareTo(vdot)>0){                                            //if sound ariive later > pad sound with silence
            builder = new FFmpegBuilder()   
                .overrideOutputFiles(true)                                      //-y
                .addExtraArgs("-t",delta)                                       //-t delta//read silence.avi for duration
                .addInput("silence.avi")                                        //-i silence.avi
                .addInput(f1.getAbsolutePath())                                 //-i inputV.avi           
                .addInput(f2.getAbsolutePath())                                 //-i inputS.avi            
                .setComplexFilter("[0:a][2:a]concat=n=2:v=0:a=1[a]")            //-filter_complex "[0:a][2:a]concat=n=2:v=0:a=1[a]" pad sound with silence.avi for duration delta
                .addOutput(out)                                                 //output
                .addExtraArgs("-map","1","-map","[a]")                          //-map 1 -map "[a]" //map stream in output file so that stream#0 is video and stream#1 is audio
                .done();
        }else{                                                                  //if video arrive later > skip sound equal to delta
            builder = new FFmpegBuilder()   
                .overrideOutputFiles(true)                                      //-y
                .addExtraArgs("-ss",delta)                                      //-ss delta//skip to timestamp delta
                .addInput(f2.getAbsolutePath())                                 //-i inputS.avi
                .addInput(f1.getAbsolutePath())                                 //-i inputV.avi              
                .addOutput(out)                                                 //output
                .addExtraArgs("-map","1","-map","0")                            //-map 1 -map 0 //map stream in output file so that stream#0 is video and stream#1 is audio
                .done();
        }
        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        FFmpegJob job = executor.createJob(builder);
        try{
            job.run();
        }catch(Exception E){
            System.err.println(E.getMessage());
        }
        File fileout = new File(out);
        return fileout;
    }    
    private static void delete(String out){                                     //try to delete file with this name in this project folder
        try{File of1 = new File(out);
            Files.deleteIfExists(of1.toPath());            
        }catch(Exception e){
            e.getMessage();
        }
    }
}
