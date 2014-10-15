package processing;

/* Author: http://blog.eqlbin.ru/2011/02/wave-java.html */

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import javax.sound.sampled.*;

public class WaveFile {
    public final int         NOT_SPECIFIED = AudioSystem.NOT_SPECIFIED; // -1
    private final int        INT_SIZE = 4;
    private int              sampleSize;
    private long             framesCount;
    private int              sampleRate;
    private int              channelsNum;
    private byte[]           data;      // wav bytes
    private AudioInputStream ais;
    private AudioFormat      af;

    private Clip     clip;
    private boolean canPlay;

    /**
     * Создает объект из указанного wave-файла
     *
     * @param file - wave-файл
     * @throws UnsupportedAudioFileException
     * @throws IOException
     */
    WaveFile(File file) throws UnsupportedAudioFileException, IOException
    {
        if(!file.exists()) {
            throw new FileNotFoundException(file.getAbsolutePath());
        }

        sampleSize = NOT_SPECIFIED;
        framesCount = NOT_SPECIFIED;
        sampleRate = NOT_SPECIFIED;
        data = null;
        ais = null;
        af = null;
        clip = null;
        canPlay = false;

        // получаем поток с аудио-данными
        ais = AudioSystem.getAudioInputStream(file);


        // получаем информацию о формате
        af = ais.getFormat();

        // количество кадров в файле
        framesCount = ais.getFrameLength();

        // Getting sample rate
        sampleRate = (int)af.getSampleRate();

        // размер сэмпла в байтах
        sampleSize = af.getSampleSizeInBits()/8;

        // Number of channels
        channelsNum = af.getChannels();

        // размер данных в байтах
        long dataLength = framesCount*af.getSampleSizeInBits()*af.getChannels()/8;

        // читаем в память все данные из файла разом
        data = new byte[(int) dataLength];
        ais.read(data);

        //Получаем реализацию интерфейса Clip
        //Может выкинуть LineUnavailableException
        AudioInputStream aisForPlay = AudioSystem.getAudioInputStream(file);
        try {
            clip = AudioSystem.getClip();
            //Загружаем наш звуковой поток в Clip
            //Может выкинуть IOException и LineUnavailableException
            clip.open(aisForPlay);
            clip.setFramePosition(0); //устанавливаем указатель на старт
            canPlay = true;
        } catch (LineUnavailableException e) {
            canPlay = false;
            System.out.println("I can play only 8bit and 16bit music.");
        }
    }


    /**
     * Создает объект из массива целых чисел
     *
     * @param sampleSize - количество байт занимаемых сэмплом
     * @param sampleRate - частота
     * @param channels - количество каналов
     * @param samples - массив значений (данные)
     * @throws Exception если размер сэмпла меньше, чем необходимо
     * для хранения переменной типа int
     */
    WaveFile(int sampleSize, float sampleRate, int channels, int[] samples) throws Exception {

        if(sampleSize < INT_SIZE){
            throw new Exception("sample size < int size");
        }

        this.sampleSize = sampleSize;
        this.af = new AudioFormat(sampleRate, sampleSize*8, channels, true, false);
        this.data = new byte[samples.length*sampleSize];

        // заполнение данных
        for(int i=0; i < samples.length; i++){
            setSampleInt(i, samples[i]);
        }

        framesCount = data.length / (sampleSize*af.getChannels());
        ais = new AudioInputStream(new ByteArrayInputStream(data), af, framesCount);
    }


    public boolean isCanPlay()
    {
        return canPlay;
    }

    public void play() { clip.start(); }

    public void stop() { clip.stop(); }

    /**
     * Возвращает формат аудио-данных
     *
     * @return формат
     */
    public AudioFormat getAudioFormat(){
        return af;
    }

    /**
     * Возвращает копию массива байт представляющих
     * данные wave-файла
     *
     *
     * @return массив байт
     */
    public byte[] getData() {
        return Arrays.copyOf(data, data.length);
    }

    /**
     * Возвращает количество байт которое занимает
     * один сэмпл
     *
     * @return размер сэмпла
     */
    public int getSampleSize() {
        return sampleSize;
    }

    /**
     * Возвращает продолжительность сигнала в секундах
     *
     * @return продолжительность сигнала
     */
    public double getDurationTime() {
        return getFramesCount() / getAudioFormat().getFrameRate();
    }

    /**
     * Возвращает количество фреймов (кадров) в файле
     *
     * @return количество фреймов
     */
    public long getFramesCount(){
        return framesCount;
    }

    /**
     * Сохраняет объект WaveFile в стандартный файл формата WAVE
     *
     * @param file
     * @throws IOException
     */
    public void saveFile(File file) throws IOException{
        AudioSystem.write( new AudioInputStream(new ByteArrayInputStream(data),
                af, framesCount), AudioFileFormat.Type.WAVE, file);
    }

    /**
     * Возвращает значение сэмпла по порядковому номеру. Если данные
     * записаны в 2 канала, то необходимо учитывать, что сэмплы левого и
     * правого канала чередуются. Например, сэмпл под номером один это
     * первый сэмпл левого канала, сэмпл номер два это первый сэмпл правого
     * канала, сэмпл номер три это второй сэмпл левого канала и т.д..
     *
     * @param sampleNumber - номер сэмпла, начиная с 0
     * @return значение сэмпла
     */
    public int getSampleInt(int sampleNumber) {

        if(sampleNumber < 0 || sampleNumber >= data.length/sampleSize){
            throw new IllegalArgumentException(
                    "sample number can't be < 0 or >= data.length/"
                            + sampleSize);
        }

        // массив байт для представления сэмпла
        // (в данном случае целого числа)
        //byte[] sampleBytes = new byte[sampleSize];
        byte[] sampleBytes = new byte[4]; //4byte = int

        // читаем из данных байты которые соответствуют
        // указанному номеру сэмпла
        for(int i = 0; i < sampleSize; i++){
            sampleBytes[i] = data[sampleNumber * sampleSize * channelsNum + i];
        }

        //sampleBytes = new byte[]{sampleBytes[0], sampleBytes[1], 0, 0};

        // преобразуем байты в целое число
        int sample = ByteBuffer.wrap(sampleBytes)
                .order(ByteOrder.LITTLE_ENDIAN).getInt();


        switch (sampleSize*8)
        {
            case 8:
                //sample = (Integer.MAX_VALUE << 8) | sample;
                sample = (byte)sample;
                break;
            case 16:
                //sample = (Integer.MAX_VALUE << 16) | sample;
                sample = (short)sample;
                break;
        }

        return sample;
    }

    /**
     * Устанавливает значение сэмпла
     *
     * @param sampleNumber - номер сэмпла
     * @param sampleValue - значение сэмпла
     */
    public void setSampleInt(int sampleNumber, int sampleValue){

        // представляем целое число в виде массива байт
        byte[] sampleBytes = ByteBuffer.allocate(sampleSize).
                order(ByteOrder.LITTLE_ENDIAN).putInt(sampleValue).array();

        // последовательно записываем полученные байты
        // в место, которое соответствует указанному
        // номеру сэмпла
        for(int i=0; i < sampleSize; i++){
            data[sampleNumber * sampleSize + i] = sampleBytes[i];
        }
    }

    public int getSampleRate()
    {
        return sampleRate;
    }

    public Clip getClip() { return clip; }
}