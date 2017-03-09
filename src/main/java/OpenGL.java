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
        glcanvas.setSize(columns, rows);
        glcanvas.addMouseMotionListener(new Move());
        final JFrame frame = new JFrame("DicomImage");
        frame.getContentPane().setLayout(null);
        frame.getContentPane().add(glcanvas);
        frame.getContentPane().setBackground(new Color(0,0,0));
        frame.setSize(columns, rows);
        frame.setLocationRelativeTo(null);
        frame.setUndecorated(true);
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
        GLContext gLContext = glcanvas.getContext();
        gLContext.makeCurrent();
        glu.gluOrtho2D(-columns, columns , -rows, rows );
        try {handleTheTexture(gl);}
        catch (FileNotFoundException e) {e.printStackTrace();}
        letsDraw(gl);
        //printMouseCoordinates();
    }

    private static void letsDraw(final GL2 gl) {

        gl.glBegin(GL2.GL_QUADS);
        gl.glTexCoord2d(0, 1);
        gl.glVertex3d(-columns, -rows, 0);
        gl.glTexCoord2d(0, 0);
        gl.glVertex3d(-columns, rows, 0);
        gl.glTexCoord2d(1, 0);
        gl.glVertex3d(columns, rows, 0);
        gl.glTexCoord2d(1, 1);
        gl.glVertex3d(columns, -rows, 0);
        gl.glEnd();
        gl.glFlush();
        gl.glDisable(GL2.GL_TEXTURE_2D);
    }

    private static void loadTexture() throws FileNotFoundException {
        dcm = new DICOM(new FileInputStream(new File("dcm2.dcm")));
        dcm.run("dcm1.dcm");
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
        s = dcm.getStringProperty("0028,0030");//image position
        str = s.split("\\\\");
        for (int i = 0; i < str.length; i++) {
            pixelSpacing[i] = Double.parseDouble(str[i].trim());
        }
        patientPosition = dcm.getStringProperty("0018,5100");
        System.out.println(patientPosition);
        bitsAloc = Integer.parseInt((dcm.getStringProperty("0028,0100")).trim());
        rows = Integer.parseInt((dcm.getStringProperty("0028,0010")).trim());
        columns = Integer.parseInt((dcm.getStringProperty("0028,0011")).trim());
        composeLetters();
    }

    private void handleTheTexture(final GL2 gl) throws FileNotFoundException {

        if (bitsAloc == 8) {
            //byte[] data = ((DataBufferByte) dcm.getBufferedImage().getData().getDataBuffer()).getData();
            byte[] data = createMask();
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
        } else {
            System.out.println("Error. BitsAllocated=16 is not supported.");
        }
    }

    public static byte[] createMask(){
        byte[] mask = new byte[columns*rows];
        for (int i = 0; i < mask.length; i++) {
            if (rows-((i)% rows) >= ((i)/rows)){
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
        //writeLetters(rightLetters, 0);
        //writeLetters(bottomLetters, 1);
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
