package Lab1;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.gl2.GLUT;
import ij.plugin.DICOM;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;

public class OpenGL extends GLCanvas implements GLEventListener {

    static GLUT glut = new GLUT();
    static GL2 gl;
    static String imageCoordinates;
    static String study_id;
    static GLProfile profile = null;
    static int rows;
    static int columns;
    static int bitsAloc;
    static DICOM dcm;
    static int mouseX = 0;
    static int mouseY = 0;
    static GLAutoDrawable glAD;
    static GLCanvas glcanvas;
    static double[] imageOrientation = new double[6];
    static double[] pixelSpacing = new double[2];
    static double[] imagePosition = new double[3];
    static String patientPosition;
    static double cX;
    static double cY;
    static String rightLetters;
    static String bottomLetters;
    static byte[] data;
    static byte[] mask;
    static byte[] dataRGBA;
    static JTextField tf1;
    static JTextField tf2;
    static boolean mirroring = false;
    static boolean scaling = false;
    static boolean isScaled = false;
    static int y = 0;
    static double Sy = 0;

    public static void main(String[] args) {
        try {
            loadTexture();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        profile = GLProfile.get(GLProfile.GL2);
        GLCapabilities capabilities = new GLCapabilities(profile);
        glcanvas = new GLCanvas(capabilities);
        glcanvas.repaint();
        OpenGL m = new OpenGL();
        glcanvas.addGLEventListener(m);
        glcanvas.setSize(columns*2, rows*2);
        glcanvas.addMouseMotionListener(new Move());
        glcanvas.addKeyListener(new KeyBoard());
        glcanvas.setFocusable(true);
        final JFrame frame = new JFrame("DicomImage");

        frame.getContentPane().setLayout(new FlowLayout());
        frame.getContentPane().add(glcanvas);

        JLabel label1 = new JLabel("y:");
        label1.setForeground(Color.white);
        JLabel label2 = new JLabel("Sy:");
        label2.setForeground(Color.white);

        tf1 = new JTextField();
        tf1.setColumns(3);
        tf2 = new JTextField();
        tf2.setColumns(3);

        frame.add(label1);
        frame.add(tf1);
        frame.add(label2);
        frame.add(tf2);

        frame.getContentPane().setBackground(new Color(0,0,0));
        frame.setSize(columns*2, rows*2+70);
        frame.setLocationRelativeTo(null);
        //frame.setUndecorated(true);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    public void display(GLAutoDrawable gLDrawable) {

        glAD = gLDrawable;
        GLU glu = new GLU();
        gl = gLDrawable.getGL().getGL2();
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        gl.glLoadIdentity();
        gl.glClearColor(0, 0, 0, 1);
        glu.gluOrtho2D(-columns,  columns , -rows, rows);
        try {handleTheTexture(gl);}
        catch (FileNotFoundException e) {e.printStackTrace();}
        letsDraw(gl);
        //printMouseCoordinates();
    }

    private static void letsDraw(final GL2 gl) {

        gl.glBegin(GL2.GL_QUADS);
        gl.glTexCoord2d(0, 1);
        gl.glVertex3d(0, 0, 0);
        gl.glTexCoord2d(0, 0);
        gl.glVertex3d(0, rows, 0);
        gl.glTexCoord2d(1, 0);
        gl.glVertex3d(columns, rows, 0);
        gl.glTexCoord2d(1, 1);
        gl.glVertex3d(columns, 0, 0);
        gl.glEnd();
        gl.glFlush();
        gl.glDisable(GL2.GL_TEXTURE_2D);
    }

    private static void loadTexture() throws FileNotFoundException {
        dcm = new DICOM(new FileInputStream(new File("DICOM_Image_for_Lab_2.dcm")));
        dcm.run("DICOM_Image_for_Lab_2.dcm");
        study_id = dcm.getStringProperty("0020,0010");
        String s = dcm.getStringProperty("0020,0037");//image orientation
        System.out.println(s);
        String[] str = s.split("\\\\");
        for (int i = 0; i < str.length; i++) {
            imageOrientation[i] = Double.parseDouble(str[i].trim());
        }
        s = dcm.getStringProperty("0020,0032");//image position
        System.out.println("Image position: "+s);
        str = s.split("\\\\");
        for (int i = 0; i < str.length; i++) {
            imagePosition[i] = Double.parseDouble(str[i].trim());
        }
        //s = dcm.getStringProperty("0028,0030");//image position
        //str = s.split("\\\\");
        //for (int i = 0; i < str.length; i++) {
        //    pixelSpacing[i] = Double.parseDouble(str[i].trim());
        //}
        patientPosition = dcm.getStringProperty("0018,5100");
        System.out.println(patientPosition);
        bitsAloc = Integer.parseInt((dcm.getStringProperty("0028,0100")).trim());
        rows = Integer.parseInt((dcm.getStringProperty("0028,0010")).trim());
        columns = Integer.parseInt((dcm.getStringProperty("0028,0011")).trim());
        data = ((DataBufferByte) dcm.getBufferedImage().getData().getDataBuffer()).getData();
        //composeLetters();
    }

    private void handleTheTexture(final GL2 gl) throws FileNotFoundException {

        if (bitsAloc == 8) {
            ByteBuffer buffer = ByteBuffer.allocate(columns * rows);
            ByteBuffer wrapArray = buffer.wrap(data, 0, columns * rows);
            gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, 1);
            final int[] textureID = new int[1];
            gl.glGenTextures(1, textureID, 0);
            gl.glBindTexture(GL2.GL_TEXTURE_2D, textureID[0]);
            gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
            gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
            gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPLACE);
            gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_LUMINANCE, columns, rows, 0, GL2.GL_LUMINANCE,
                    GL2.GL_UNSIGNED_BYTE, wrapArray);
            gl.glActiveTexture(GL2.GL_TEXTURE0);
            gl.glEnable(GL2.GL_TEXTURE_2D);
            if(mirroring){
                gl.glScaled(-1,1,1);
            }else{
                gl.glScaled(1,1,1);
            }
            if(scaling){
                getTfData();
                gl.glTranslated(0,y,0);
                gl.glScaled(1, Sy, 1);
                gl.glTranslated(0,-y,0);
                isScaled = true;
            }else if(!scaling & isScaled){
                gl.glTranslated(0,y,0);
                gl.glScaled(1, 1, 1);
                gl.glTranslated(0,-y,0);
                isScaled = false;
            }
        } else {
            ByteBuffer buffer = ByteBuffer.allocate(columns * rows * 4);
            ByteBuffer wrapArray = buffer.wrap(dataRGBA, 0, columns * rows * 4);
            gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, 1);
            final int[] textureID = new int[1];
            gl.glGenTextures(1, textureID, 0);
            gl.glBindTexture(GL2.GL_TEXTURE_2D, textureID[0]);
            gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
            gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
            gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPLACE);
            gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGBA, columns, rows, 0, GL2.GL_RGBA,
                    GL2.GL_UNSIGNED_BYTE, wrapArray);
            gl.glActiveTexture(GL2.GL_TEXTURE0);
            gl.glEnable(GL2.GL_TEXTURE_2D);
            if(mirroring){
                gl.glScaled(-1,1,1);
            }else{
                gl.glScaled(1,1,1);
            }
        }
    }

    public static void monoToRGBA(){
        data = ((DataBufferByte) dcm.getBufferedImage().getData().getDataBuffer()).getData();
        dataRGBA = new byte[columns * rows * 4];
        for (int i = 4; i < dataRGBA.length; i+=4) {
            dataRGBA[i-4]=0;
            dataRGBA[i-3]=data[(i/4)-1];
            dataRGBA[i-2]=0;
            dataRGBA[i-1]=0;
        }
    }

    public static void getTfData(){
        try{
            y = Integer.parseInt(tf1.getText());
            Sy = Double.parseDouble(tf2.getText());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static byte[] createMask(){
        byte[] mask = new byte[columns*rows];
        for (int i = 0; i < mask.length; i++) {
            if (rows-((i)% rows) > ((i)/rows)){
                mask[i] = (byte)255;
            }else {
                mask[i] = 0;
            }
        }
        return mask;
    }

    public static void glWrite(float x, float y, int font, String txt, int kls) {
        gl.glColor3f(0f,1f,0f);
        char[] text = txt.toCharArray();
        gl.glRasterPos2f(x, y);
        for (int i=0; i<kls; i++)
            glut.glutBitmapCharacter(font, text[i]);
    }

        public static void composeLetters(){
        //правая
        rightLetters = "L";
        if ((imageOrientation[1]>0) && (imageOrientation[1]<1)){
            rightLetters += "P";
        }else if(imageOrientation[1]<0){
            rightLetters += "A";
        }
        if ((imageOrientation[2]>0) && (imageOrientation[2]<1)){
            rightLetters += "H";
        }else if(imageOrientation[2]<0){
            rightLetters += "F";
        }

        //нижняя
        bottomLetters = "A";
        if ((imageOrientation[3]>0) && (imageOrientation[3]<1)){
            bottomLetters += "R";
        }else if(imageOrientation[3]<0){
            bottomLetters += "L";
        }
        if ((imageOrientation[5]>0) && (imageOrientation[5]<1)){
            bottomLetters += "F";
        }else if(imageOrientation[5]<0){
            bottomLetters += "H";
        }
    }

    public static void writeLetters(String leters, int position){
        char[] text = leters.toCharArray();
        if (position == 0){ //справа
            gl.glRasterPos2f(-240, 128);
            for (int i=0; i<text.length; i++)
                glut.glutBitmapCharacter(GLUT.BITMAP_8_BY_13, text[i]);
        }else{ //снизу
            gl.glRasterPos2f(-100, 240);
            for (int i=0; i<text.length; i++)
                glut.glutBitmapCharacter(GLUT.BITMAP_8_BY_13, text[i]);
        }
    }

    public static void printMouseCoordinates() {
        if (cX<rows && cY<columns) {
            double x = imagePosition[0] + (mouseX * pixelSpacing[1] * imageOrientation[4]);
            double y = imagePosition[1] + (mouseY * pixelSpacing[0] * imageOrientation[0]);
            double z = imagePosition[2] + mouseX * imageOrientation[2] + mouseY * imageOrientation[5];

            imageCoordinates =
                    "x=" + mouseX
                            + ";y=" + mouseY
                            + ";z=0   "
                            + "X=" + String.format("%.2f", x) + "mm;"
                            + "Y=" + String.format("%.2f", y) + "mm;"
                            + "Z=" + String.format("%.2f", z) + "mm";
        }else {
            imageCoordinates = "out of image zone.";
        } glWrite(-200, -127, GLUT.BITMAP_8_BY_13, imageCoordinates, imageCoordinates.length());
    }

    public void init(GLAutoDrawable gLDrawable) {}

    private static class KeyBoard implements KeyListener{
        @Override
        public void keyTyped(KeyEvent e) {

        }

        @Override
        public void keyPressed(KeyEvent e) {
            if(e.getKeyCode()== KeyEvent.VK_1 | e.getKeyCode() == KeyEvent.VK_NUMPAD1){
                bitsAloc = 8;
                data = ((DataBufferByte) dcm.getBufferedImage().getData().getDataBuffer()).getData();
                glcanvas.repaint();
            }else if(e.getKeyCode()== KeyEvent.VK_2 | e.getKeyCode() == KeyEvent.VK_NUMPAD2){
                bitsAloc = 8;
                mask = createMask();
                data = ((DataBufferByte) dcm.getBufferedImage().getData().getDataBuffer()).getData();
                for (int i = 0; i < data.length; i++) {
                    data[i] = (byte)(data[i]&mask[i]);
                }
                glcanvas.repaint();
            }else if(e.getKeyCode()== KeyEvent.VK_3 | e.getKeyCode() == KeyEvent.VK_NUMPAD3){
                bitsAloc = 16;
                monoToRGBA();
                glcanvas.repaint();
            }else if (e.getKeyCode() == KeyEvent.VK_ESCAPE){
                System.exit(0);
            }else if(e.getKeyCode()==KeyEvent.VK_Z){
                if(mirroring){
                    mirroring = false;
                }else {
                    mirroring = true;
                }
                glcanvas.repaint();
            }else if(e.getKeyCode()==KeyEvent.VK_X){
                //TODO маштабирование
                if(scaling){
                    scaling = false;
                }else {
                    scaling = true;
                }
                glcanvas.repaint();
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {

        }
    }

    private static class Move implements MouseMotionListener{
        @Override
        public void mouseDragged(MouseEvent e) {}

        @Override
        public void mouseMoved(MouseEvent e) {
            //cX = e.getX();
            //cY = e.getY();
            //mouseX = -(rows - e.getX());
            //mouseY = columns - e.getY();
            //printMouseCoordinates();
            //glcanvas.repaint();
        }
    }

    @Override
    public void dispose(GLAutoDrawable glAutoDrawable) {}

    public void reshape(GLAutoDrawable gLDrawable, int x, int y, int width, int height) {}
}
