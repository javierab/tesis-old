import java.io.*;
import java.util.List;
import java.lang.Integer;

public class heatmap
{

    public static void main( String [] args )
    {
	int[][] contacts = new int[60][60];
	int n1 = 0;
	int n2 = 0;
        try
        {
            BufferedReader br = new BufferedReader(new FileReader(args[0]));
            String fileRead = br.readLine();
            while (fileRead != null)
            {

                String[] sp = fileRead.split(" ");
		if(sp.length != 5 || sp[4] == "down"){ System.out.println("linea mala: " + sp.length);}
		else{
               		n1 = Integer.parseInt(sp[2]);
			n2 = Integer.parseInt(sp[3]);
			contacts[n1][n2]++;
			System.out.println("++ for " + n1 + ", " + n2);
		}
                fileRead = br.readLine();
            }
            br.close();
        }
        catch (FileNotFoundException fnfe)
        {
            System.out.println("file not found");
        }

        catch (IOException ioe)
        {
            ioe.printStackTrace();
        }
	for(int i = 0; i < 60; i++)
		System.out.print(i + ", ");
	System.out.println("");
        for(int i = 0; i < 60; i++){
		System.out.print(i + ", ");
		for(int j=0; j< 59; j++){
			System.out.print(contacts[i][j] + ", ");
		}
		System.out.println(contacts[i][59]);
	}

    }

}
