package edu.ipn.escom.apcr.ServidorAdivinaQuien.Juego;

import java.awt.BufferCapabilities.FlipContents;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import edu.ipn.escom.apcr.ServidorAdivinaQuien.Beans.Personaje;
import edu.ipn.escom.apcr.ServidorAdivinaQuien.Util.AdministradorDePersonajes;
import edu.ipn.escom.apcr.ServidorAdivinaQuien.Util.ReconocimientoDeVoz;
import io.grpc.netty.shaded.io.netty.util.internal.StringUtil;

public class ServidorAdivinaQuien {

	private static boolean EVER = true;
		
	private ServerSocket socketServidor;
	private int puerto;
	
	private LinkedList<Socket> socketsClientes = new LinkedList<Socket>();
	private LinkedList<DataInputStream> flujosDeEntradaDeClientes = new LinkedList<DataInputStream>();
	private LinkedList<DataOutputStream> flujosDeSalidaDeClientes = new LinkedList<DataOutputStream>();
	
	private LinkedList<Thread> hilosAtencionClientes = new LinkedList<Thread>();

	private LinkedList<Personaje> personajes;
	private Personaje personajeElegidoPartida;
	private boolean finDelJuego = false;
	
	private volatile Integer turnoActual = 0;
	private int numeroDeJugadores;
	
	public void iniciarServidor() throws JDOMException, IOException, InterruptedException {
		mostrarMensajeBienvenida();
		solicitarPuerto();
		solicitarNumeroDeJugadores();
		inicializarConexionServidor();
		personajes = AdministradorDePersonajes.obtenerPersonajesDeArchivo();
		for(;EVER;) {
			
			System.out.println("Esperando jugadores...");
			//Escojemos un personaje al azar para la partida
			personajeElegidoPartida = personajes.get(new Random().nextInt(personajes.size()));
			//Esperamos que se conecten todos los jugadores
			int numeroJugador = 0;
			while(numeroJugador<numeroDeJugadores) {
				Socket socketCliente = socketServidor.accept();
				socketsClientes.add(socketCliente);
				DataInputStream flujoEntradaCliente = new DataInputStream(socketCliente.getInputStream());
				flujosDeEntradaDeClientes.add(flujoEntradaCliente);
				DataOutputStream flujoSalidaCliente = new DataOutputStream(socketCliente.getOutputStream());
				flujosDeSalidaDeClientes.add(flujoSalidaCliente);
				//Enviamos el numero de jugador
				flujoSalidaCliente.writeUTF(String.valueOf(numeroJugador));
				enviarXMLPersonajes(numeroJugador);
				System.out.println((numeroJugador+1)+" de "+numeroDeJugadores+" jugadores conectados");
				enviarDatoTodosLosClientes("JUGADOR_CONECTADO "+socketsClientes.size()+" "+numeroDeJugadores);
				numeroJugador++;
			}
			System.out.println("Iniciando partida...");
			enviarDatoTodosLosClientes("PARTIDA_POR_EMPEZAR");
			Thread.sleep(3000);
			enviarDatoTodosLosClientes("PARTIDA_COMIENZA");
			
			for(numeroJugador = 0;numeroJugador<numeroDeJugadores;numeroJugador++) {
				hilosAtencionClientes.add(iniciarHiloAtencionCliente(numeroJugador));
			}
			System.out.println("Partida iniciada");
			for(Thread hiloAtencionCliente:hilosAtencionClientes) {
				try {
					hiloAtencionCliente.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			cerrarConexionesClientes();
			System.out.println("Partida terminada, reiniciando...");
		}
	}
	
	private Thread iniciarHiloAtencionCliente(final int numeroCliente) {
		Thread hiloAtencionCliente = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					atenderCliente(numeroCliente);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		hiloAtencionCliente.start();
		return hiloAtencionCliente;
	}
	
	private void atenderCliente(int numeroCliente) throws Exception {
		while (!finDelJuego) {
			synchronized (turnoActual) {
				if(turnoActual == numeroCliente) {
					enviarDatoTodosLosClientes("TURNO_DE "+turnoActual);
					//Manejamos el dato recibido
					String datoRecibido = flujosDeEntradaDeClientes.get(numeroCliente).readUTF().trim();
					if(datoRecibido.equals("TIRO-CLIENTE")) {
						File archivoAudioTiro = File.createTempFile("AUDIO_TIRO_CLIENTE_"+numeroCliente+"_",".wav");
						recibirArchivoCliente(archivoAudioTiro, numeroCliente);
						//Realizamos el procesamiento de el audio
						String resultadoReconocimiento = StringUtils.stripAccents(ReconocimientoDeVoz.reconocerAudioDeArchivo(archivoAudioTiro));
						String respuestaServidor = "";
						//Primero verificamos si se dijo el nombre
						if(resultadoReconocimiento.toUpperCase().contains(personajeElegidoPartida.getNombre().toUpperCase())) {
							respuestaServidor = "El personaje es "+personajeElegidoPartida.getNombre()+", el jugador "+(numeroCliente+1)+" GANO!";
							finDelJuego = true;
						}else {
							//Verificamos caracteristicas
							boolean tieneAlgunaCaracteristica = false;
							for(String caracteristicaPersonajeElegido:personajeElegidoPartida.getCaracteristicas()) {
								if(resultadoReconocimiento.toUpperCase().contains(caracteristicaPersonajeElegido.toUpperCase())) {
									tieneAlgunaCaracteristica = true;
								}
							}
							if(tieneAlgunaCaracteristica) {
								respuestaServidor = "Si";
							}else {
								respuestaServidor = "No";
							}
						}
						enviarDatoTodosLosClientes("TIRO-Jugador "+(numeroCliente+1)+"\n \t Tiro: "+resultadoReconocimiento+"\n \t Respuesta: "+respuestaServidor+"\n");	
					}
					//Ya termino nuestro turno, pasamos el turno al siguiene jugador
					if(numeroCliente == numeroDeJugadores-1 ) {
						turnoActual = 0;
					}else {
						turnoActual++;
					}
				}
			}
		}
	}
	
	private void enviarDatoTodosLosClientes(String dato) throws IOException {
		for(DataOutputStream flujoSalidaClientes:flujosDeSalidaDeClientes) {
			flujoSalidaClientes.writeUTF(dato);
		}
	}
	
	private void mostrarMensajeBienvenida() {
		System.out.println();
		System.out.println("****************************************************");
		System.out.println("*                                                  *");
		System.out.println("* Bienvenido al servidor de adivina quien          *");
		System.out.println("*                                                  *");
		System.out.println("* Creado por:                                      *");
		System.out.println("*     Balderas Aceves Lidia Lizbeth                *");
		System.out.println("*     Corona Lopez Emilio Abraham                  *");
		System.out.println("*                                                  *");
		System.out.println("* Materia: Aplicaciones para comunicaciones en red *");
		System.out.println("*                                                  *");
		System.out.println("* Semestre: 2020-A                                 *");
		System.out.println("*                                                  *");
		System.out.println("* Grupo: 3CM8                                      *");
		System.out.println("*                                                  *");
		System.out.println("****************************************************");
		System.out.println();
	}
	
	
	private void inicializarConexionServidor() throws IOException {
		socketServidor = new ServerSocket(puerto);
	}
	
	private void solicitarPuerto() {
		boolean continua = true;
		int numeroJugadores = 0;
		while(continua) {
			System.out.println("Ingrese el puerto de escucha: ");
			Scanner scn = new Scanner(System.in);
			puerto = scn.nextInt();
			if(puerto>999) {
				continua = false;
			}else {
				System.out.println("El puerto ingresado no es valido");
			}
		}

	}

	private void solicitarNumeroDeJugadores() {
		boolean continua = true;
		while(continua) {
			System.out.println("Ingrese el numero minimo de jugadores: ");
			Scanner scn = new Scanner(System.in);
			numeroDeJugadores = scn.nextInt();
			if(numeroDeJugadores>0) {
				continua = false;
			}else {
				System.out.println("El numero de jugadores debe de ser mayor que cero");
			}
		}
	}
	
	private void enviarXMLPersonajes(int numeroCliente) throws FileNotFoundException, IOException {
		File archivoXMLPersonajes = new File(AdministradorDePersonajes.RUTA_ARCHIVOS+File.separator+AdministradorDePersonajes.ARCHIVO_XML_PERSONAJES);
		enviarArchivoCliente(numeroCliente, archivoXMLPersonajes);
	}
	
	private void recibirArchivoCliente(File archivo,int numeroCliente) throws IOException {
		FileOutputStream fileOutputStream = new FileOutputStream(archivo);
		long tamanoArchivo = flujosDeEntradaDeClientes.get(numeroCliente).readLong();
		int bytesLeidos = 0;
		byte[] buffer = new byte[4092];
		while (tamanoArchivo > 0 && (bytesLeidos = socketsClientes.get(numeroCliente).getInputStream().read(buffer, 0, (int)Math.min(buffer.length, tamanoArchivo))) != -1){
			fileOutputStream.write(buffer,0,bytesLeidos);
		  tamanoArchivo -= bytesLeidos;
		}
		fileOutputStream.close();
	}
	
	private void enviarArchivoCliente(int numeroCliente, File archivoAEnviar) throws IOException {
		FileInputStream fileInputStream = new FileInputStream(archivoAEnviar);
		long tamanoArchivo = archivoAEnviar.length();
		flujosDeSalidaDeClientes.get(numeroCliente).writeLong(tamanoArchivo);
		int bytesLeidos = 0;
		byte[] buffer = new byte[4092];
		while (tamanoArchivo > 0 && (bytesLeidos = fileInputStream.read(buffer, 0, (int)Math.min(buffer.length, tamanoArchivo))) != -1){
		  socketsClientes.get(numeroCliente).getOutputStream().write(buffer,0,bytesLeidos);
		  tamanoArchivo -= bytesLeidos;
		}
		fileInputStream.close();
	}
	
	private void cerrarConexionesClientes() throws IOException {
		for(Socket socketCliente:socketsClientes) {
			socketCliente.close();
		}
		socketsClientes.clear();
		flujosDeEntradaDeClientes.clear();
		flujosDeSalidaDeClientes.clear();
	}
}
