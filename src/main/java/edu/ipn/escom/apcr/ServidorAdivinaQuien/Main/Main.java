package edu.ipn.escom.apcr.ServidorAdivinaQuien.Main;

import java.io.IOException;

import org.jdom.JDOMException;

import edu.ipn.escom.apcr.ServidorAdivinaQuien.Juego.ServidorAdivinaQuien;
import edu.ipn.escom.apcr.ServidorAdivinaQuien.Util.AdministradorDePersonajes;

public class Main {
	
	public static void main(String[] args) {
		if(args.length>0) {
			if(args[0].trim().equals("-mp")) {
				//Iniciamos modo de mantenimiento de personajes
				new AdministradorDePersonajes().iniciarAdministrador();
			}
		}else {
			try {
				//Iniciamos el servidor
				new ServidorAdivinaQuien().iniciarServidor();
			} catch (JDOMException | IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
}
