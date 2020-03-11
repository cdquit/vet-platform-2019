package handin2;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class VetGUI extends JPanel implements ActionListener {
    private AnimalProcessor animalQueue;
    private JPanel animalPanel;
    private JButton newPatient, release, seeLater, updatePic, loadXML, saveXML;
    private JLabel numAnimalsLeft;
    public VetGUI()
    {
        super(new BorderLayout());
        
        animalQueue = new AnimalProcessor();
        animalPanel = new JPanel(new GridLayout(1, 1)); //let layout manager handles the panel eg. sizing
        add(animalPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 6));
        newPatient = new JButton("New Patient");
        newPatient.addActionListener(this);
        buttonPanel.add(newPatient);
        release = new JButton("Release");
        release.addActionListener(this);
        buttonPanel.add(release);
        seeLater = new JButton("See Later");
        seeLater.addActionListener(this);
        buttonPanel.add(seeLater);
        updatePic = new JButton("Update Picture");
        updatePic.addActionListener(this);
        buttonPanel.add(updatePic);
        loadXML = new JButton("Load XML");
        loadXML.addActionListener(this);
        buttonPanel.add(loadXML);
        saveXML = new JButton("Save XML");
        saveXML.addActionListener(this);
        buttonPanel.add(saveXML);
        add(buttonPanel, BorderLayout.SOUTH);

        numAnimalsLeft = new JLabel("", SwingConstants.CENTER);
        add(numAnimalsLeft, BorderLayout.NORTH);
        
        updatePanel(animalQueue.animalsLeftToProcess());
    }
    
    //private method to update the animalPanel whenever an action is performed
    //animalPanel is the displayPanel of the top animal
    private void updatePanel(int numAnimals)
    {
        animalPanel.removeAll();
        
        if (numAnimals != 0)
        {
            animalQueue.getNextAnimal().updateDate(new Date());
            animalPanel.add(animalQueue.getNextAnimal().getDisplayPanel());
        }
        else
        {
            AnimalPatient emptyAnimal = new AnimalPatient("", ""); //empty animal to dispaly empty panel
            animalPanel.add(emptyAnimal.getDisplayPanel());
        }
        
        animalPanel.revalidate();
        animalPanel.repaint();
        numAnimalsLeft.setText("Animals still waiting to be seen: " + animalQueue.animalsLeftToProcess());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        
        if (source == newPatient)
        {
            String type = JOptionPane.showInputDialog(this, "Animal species?", "WHAT IS YOUR ANIMAL", JOptionPane.PLAIN_MESSAGE);
            String name = JOptionPane.showInputDialog(this, "Animal name?", "WHAT IS YOUR ANIMAL", JOptionPane.PLAIN_MESSAGE);
            if (type != null && name != null && !type.equals("") && !name.equals("")) //if type or name is null or empty, it will not be added
                animalQueue.addAnimal(new AnimalPatient(type, name));
        }
        else if (source == release)
        {
            if (animalQueue.animalsLeftToProcess() != 0)
            {
                animalQueue.getNextAnimal().updateDate(new Date());
                animalQueue.releaseAnimal();
            }
        }
        else if (source == seeLater)
        {
            if (animalQueue.animalsLeftToProcess() != 0)
            {
                animalQueue.getNextAnimal().updateDate(new Date());
                animalQueue.addAnimal(animalQueue.releaseAnimal());
            }
        }
        else if (source == updatePic)
        {
            JFileChooser chooser = new JFileChooser(new File("."));
            int status = chooser.showOpenDialog(this);
            
            if (status == JFileChooser.APPROVE_OPTION)
            {
                File file = chooser.getSelectedFile();
                String location = file.getPath();
                if (animalQueue.animalsLeftToProcess() != 0)
                    animalQueue.getNextAnimal().loadImage(location);
            }
        }
        else if (source == loadXML)
        {
            JFileChooser chooser = new JFileChooser(new File("."));
            int status = chooser.showOpenDialog(this);
            
            if (status == JFileChooser.APPROVE_OPTION)
            {
                File file = chooser.getSelectedFile();
                try
                {
                    FileInputStream fis = new FileInputStream(file);
                    String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
                    String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema"; 
                    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                    builderFactory.setNamespaceAware(true);
                    builderFactory.setValidating(true);
                    builderFactory.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
                    
                    DocumentBuilder builder = builderFactory.newDocumentBuilder();
                    animalQueue.loadAnimalsFromXML(builder.parse(fis));
                    fis.close();
                }
                catch (ParserConfigurationException ex)
                {
                    System.out.println("Can't get builder: " + ex);
                }
                catch (SAXException ex)
                {
                    System.out.println("Error parsing document: " + ex);
                }
                catch (IOException ex)
                {
                    System.out.println("IO exception: " + ex);
                }
            }
        }
        else if (source == saveXML)
        {
            JFileChooser chooser = new JFileChooser(new File("."));
            int status = chooser.showSaveDialog(this);
            
            if (status == JFileChooser.APPROVE_OPTION)
            {
                try
                {
                    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = builderFactory.newDocumentBuilder();
                    Document document = builder.newDocument();
                    animalQueue.saveAnimalsToXML(document);
                    
                    FileOutputStream fos = new FileOutputStream(chooser.getSelectedFile());
                    TransformerFactory transFactory = TransformerFactory.newInstance();
                    Transformer transformer = transFactory.newTransformer();
                    transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //set indent to true
                    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "3"); //the amounnt of indentation, works when indent is true
                    transformer.transform(new DOMSource(document), new StreamResult(fos));
                    fos.close();
                }
                catch (TransformerConfigurationException ex)
                {
                    System.out.println("Can't get transformer: " + ex);
                }
                catch (TransformerException ex)
                {
                    System.out.println("Exception transforming XML: " + ex);
                }
                catch (IOException ex)
                {
                    System.out.println("IO exception: " + ex);
                }
                catch (ParserConfigurationException ex)
                {
                    Logger.getLogger(VetGUI.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        updatePanel(animalQueue.animalsLeftToProcess());
    }
	
    public static void main(String[] args)
    {
        JFrame frame = new JFrame("Veterinarian");
        // kill all threads when frame closes
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.getContentPane().add(new VetGUI(frame));
        frame.getContentPane().add(new VetGUI());
        frame.pack();
        // position the frame in the middle of the screen
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screenDimension = tk.getScreenSize();
        Dimension frameDimension = frame.getSize();
        frame.setLocation((screenDimension.width-frameDimension.width)/2,
           (screenDimension.height-frameDimension.height)/2);
        frame.setVisible(true);
    }
}
