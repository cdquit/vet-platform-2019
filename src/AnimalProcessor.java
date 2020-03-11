package handin2;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.NoSuchElementException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

//max heap
public class AnimalProcessor {
    private final int INITIAL_CAPACITY = 15;
    private AnimalPatient[] waitlist;
    private int numElements;

    public AnimalProcessor()
    {
    	waitlist = new AnimalPatient[INITIAL_CAPACITY];
    	numElements = 0;
    }

    public void addAnimal(AnimalPatient animal)
    {
    	if (numElements == waitlist.length)
            expandCapacity();
    	
    	waitlist[numElements++] = animal;
    	if (numElements > 1)
            restoreHeapAfterAdd(numElements - 1);
    }

    private void restoreHeapAfterAdd(int childIndex)
    {
    	if (childIndex > 0) //keep traversing iff the restore process continues and we haven't reached the rootNode
    	{
            int parentIndex = (childIndex - 1) / 2;
            if (waitlist[childIndex].compareTo(waitlist[parentIndex]) > 0) //if child larger bubble up, else stop traversing (restoring)
            {
                AnimalPatient temp = waitlist[childIndex];
                waitlist[childIndex] = waitlist[parentIndex];
                waitlist[parentIndex] = temp;
                restoreHeapAfterAdd(parentIndex);
            }
    	}
    }
    
    private void expandCapacity()
    {
    	AnimalPatient[] largerArray = new AnimalPatient[waitlist.length * 2];
    	int index = 0;
    	for (AnimalPatient animal : waitlist)
            largerArray[index++] = animal;
    	waitlist = largerArray;
    }
    
    public AnimalPatient getNextAnimal()
    {
    	if (numElements == 0)
            throw new NoSuchElementException("Empty queue");
    	return waitlist[0];
    }

    public AnimalPatient releaseAnimal()
    {
    	if (numElements > 0)
    	{
            AnimalPatient animal = waitlist[0];
            waitlist[0] = waitlist[--numElements];
            waitlist[numElements] = null;

            if (numElements > 1)
                restoreHeapAfterRemove(0);

            return animal;
    	}
    	else
            throw new NoSuchElementException("Empty queue");
    	
    }
    
    private void restoreHeapAfterRemove(int parentIndex)
    {
        int largerIndex = -1;
        int leftChildIndex = 2 * parentIndex + 1;
        int rightChildIndex = 2 * parentIndex + 2;

        if (leftChildIndex < numElements) //if false parent is leaf and so stop restoring, we reached the end
        {
            if (rightChildIndex < numElements) //if false parent has left child only
            {
                //parent has left and right children
                //find the larger child of left and right children
                if (waitlist[leftChildIndex].compareTo(waitlist[rightChildIndex]) > 0) 
                    largerIndex = leftChildIndex;
                else
                    largerIndex = rightChildIndex;
            }
            else
                largerIndex = leftChildIndex;

            if (waitlist[parentIndex].compareTo(waitlist[largerIndex]) < 0) //if parent smaller bubble down, else stop traversing (restoring)
            {
                AnimalPatient temp = waitlist[largerIndex];
                waitlist[largerIndex] = waitlist[parentIndex];
                waitlist[parentIndex] = temp;
                restoreHeapAfterRemove(largerIndex);
            }
        }
    }

    public int animalsLeftToProcess()
    {
    	return numElements;
    }

    public void loadAnimalsFromXML(Document document)
    {
        //clear animal patient array
        for (int k = 0; k < numElements; k++)
            waitlist[k] = null;
        numElements = 0;
        document.getDocumentElement().normalize();
        Node rootXMLNode = document.getDocumentElement(); //at animals tag
        DOMUtilities utility = new DOMUtilities();
        Collection<Node> animalNodes = utility.getAllChildNodes(rootXMLNode, "animal");
        for (Node animal: animalNodes)
        {
            try
            {
                //extract name - required
                String name = utility.getAttributeString(animal, "name");
                //extract species - required
                String species = utility.getAttributeString(animal, "species");
                //extract dateLastSeen - optional
                Collection<Node> dateSeenNodes = utility.getAllChildNodes(animal, "dateSeen");
                Date date = new Date();
                //make sure dateSeenNodes is not empty
                if (dateSeenNodes.size() > 0)
                {
                    String dateSeen = utility.getTextContent(dateSeenNodes.iterator().next());
                    if (dateSeen != null)
                        date = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").parse(dateSeen); //parse dateSeen
                }
                AnimalPatient patient = new AnimalPatient(species, name, date); //create animal patient
                //extract picURL - optional
                Collection<Node> picURLNodes = utility.getAllChildNodes(animal, "picURL");
                if (picURLNodes.size() > 0)
                {
                    String picURL = utility.getTextContent(picURLNodes.iterator().next());
                    patient.loadImage(picURL);
                }
                //extract symptoms - optional
                Collection<Node> symptomsNodes = utility.getAllChildNodes(animal, "symptoms");
                if (!symptomsNodes.isEmpty())
                {
                    String symptoms = utility.getTextContent(symptomsNodes.iterator().next());
                    patient.setSymptoms(symptoms);
                }
                //extract treatment - optional
                Collection<Node> treatmentNodes = utility.getAllChildNodes(animal, "treatment");
                if (!treatmentNodes.isEmpty())
                {
                    String treatment = utility.getTextContent(treatmentNodes.iterator().next());
                    patient.setTreatment(treatment);
                }
                //extract priority - optional
                String priority = utility.getAttributeString(animal, "priority");
                if (priority != null)
                    patient.setPriority(Integer.parseInt(priority));
                addAnimal(patient);
            }
            catch (ParseException ex)
            {
                System.out.println("Error parsing date: " + ex);
            }
        }
    }
    
    public void saveAnimalsToXML(Document document)
    {
        Element root = document.createElement("animals");
        root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        root.setAttribute("xsi:noNamespaceSchemaLocation", "animals.xsd");
        document.appendChild(root); //append firstChild to document
        
        Element description = document.createElement("description");
        description.appendChild(document.createTextNode("Yesterday's Animals"));
        root.appendChild(description);
        
        for (int k = 0; k < numElements; k++)
        {
            Element animal = document.createElement("animal");
            animal.setAttribute("name", waitlist[k].getName());
            animal.setAttribute("species", waitlist[k].getSpecies());
            animal.setAttribute("priority", String.valueOf(waitlist[k].getPriority()));

            //create picURL
            if (waitlist[k].getImageLocation() != null)
            {
                Element picURL = document.createElement("picURL");
                picURL.appendChild(document.createTextNode(waitlist[k].getImageLocation()));
                animal.appendChild(picURL);
            }
            
            //create symptoms tag
            if (!waitlist[k].getSymptoms().isEmpty() && !waitlist[k].getSymptoms().equalsIgnoreCase("unknown"))
            {
                Element symptoms = document.createElement("symptoms");
                symptoms.appendChild(document.createTextNode(waitlist[k].getSymptoms()));
                animal.appendChild(symptoms);
            }
            
            //create treatment
            if (!waitlist[k].getTreatment().isEmpty() && !waitlist[k].getTreatment().equalsIgnoreCase("unknown"))
            {
                Element treatment = document.createElement("treatment");
                treatment.appendChild(document.createTextNode(waitlist[k].getTreatment()));
                animal.appendChild(treatment);
            }
            
            //create dateLastSeen
            Element dateSeen = document.createElement("dateSeen");
            dateSeen.appendChild(document.createTextNode(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(waitlist[k].getDate())));
            animal.appendChild(dateSeen);
            
            root.appendChild(animal);
        }
    }
    
    //driver method to test that the animals are removed in order of priority
    //then by dateSeen
    public static void main(String[] args)
    {
        try
        {
            System.out.println("Animals added in random order:");
            AnimalProcessor list = new AnimalProcessor();
            AnimalPatient animal = new AnimalPatient("A", "A");
            animal.setPriority(4);
            list.addAnimal(animal);
            System.out.println(animal);
            animal = new AnimalPatient("B", "B", new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").parse("06-05-2019 06:05:19"));
            animal.setPriority(7);
            list.addAnimal(animal);
            System.out.println(animal);
            animal = new AnimalPatient("C", "C");
            animal.setPriority(5);
            list.addAnimal(animal);
            System.out.println(animal);
            animal = new AnimalPatient("D", "D");
            animal.setPriority(2);
            list.addAnimal(animal);
            System.out.println(animal);
            animal = new AnimalPatient("E", "E", new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").parse("04-05-2019 04:05:19"));
            animal.setPriority(7);
            list.addAnimal(animal);
            System.out.println(animal);
            
            System.out.println("\nAnimals removed in order by priority then by date last seen:");
            int numElements = list.animalsLeftToProcess();
            for (int k = 0; k < numElements; k++)
                System.out.println(list.releaseAnimal());
        }
        catch (ParseException ex)
        {
            System.out.println("Parsing error: " + ex);
        }
    }
}
