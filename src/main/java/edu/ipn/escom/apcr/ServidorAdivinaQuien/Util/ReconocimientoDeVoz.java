package edu.ipn.escom.apcr.ServidorAdivinaQuien.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognizeRequest;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.speech.v1.SpeechSettings;
import com.google.protobuf.ByteString;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ReconocimientoDeVoz {

    public static String reconocerAudioDeArchivo(File archivoAudio) {
    	String textoReconocido = "";

    	try {
        	CredentialsProvider proveedorCredenciales = FixedCredentialsProvider.create(ServiceAccountCredentials.fromStream(ReconocimientoDeVoz.class.getResourceAsStream("/GoogleCloudCredentials/adivinaquien-d0fc1f5ffc9e.json")));
        	SpeechSettings configuracionVoz = SpeechSettings.newBuilder().setCredentialsProvider(proveedorCredenciales).build();
    		SpeechClient clienteReconocimientoVoz = SpeechClient.create(configuracionVoz);
    		RecognitionConfig configuracionReconocimientoVoz = RecognitionConfig.newBuilder().setLanguageCode("es-US").setSampleRateHertz(16000).setEncoding(RecognitionConfig.AudioEncoding.LINEAR16).build();
    		Path rutaArchivoAudio = Paths.get(archivoAudio.getAbsolutePath());
    		byte[] bytesArchivoAudio = Files.readAllBytes(rutaArchivoAudio);
    		ByteString cadenaBytesArchivoAudio = ByteString.copyFrom(bytesArchivoAudio);
    		RecognitionAudio audio = RecognitionAudio.newBuilder().setContent(cadenaBytesArchivoAudio).build();
    		RecognizeRequest peticionDeReconocimientoDeVoz = RecognizeRequest.newBuilder().setConfig(configuracionReconocimientoVoz).setAudio(audio).build();
    		RecognizeResponse respuestaDeReconocimientoDeVoz = clienteReconocimientoVoz.recognize(peticionDeReconocimientoDeVoz);
    		for (SpeechRecognitionResult resultadoReconocimientoVoz : respuestaDeReconocimientoDeVoz.getResultsList()) {
    			SpeechRecognitionAlternative alternativaReconocimientoVoz = resultadoReconocimientoVoz.getAlternativesList().get(0);
    			textoReconocido = alternativaReconocimientoVoz.getTranscript();
    		}
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	return textoReconocido;
    }
}
