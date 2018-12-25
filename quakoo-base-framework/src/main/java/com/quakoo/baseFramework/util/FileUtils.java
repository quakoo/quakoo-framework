package com.quakoo.baseFramework.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicInteger;

public class FileUtils {

    public static final ByteBuffer decreaseBufferCapatity(final ByteBuffer byteBuffer, final int capacity) {
        if (byteBuffer == null) {
            throw new IllegalArgumentException("buffer is null");
        }

        if (capacity < byteBuffer.position()) {
            return byteBuffer;
        }
        final ByteBuffer result = byteBuffer.isDirect() ? ByteBuffer.allocateDirect(capacity) : ByteBuffer
                .allocate(capacity);
        result.order(byteBuffer.order());
        byteBuffer.flip();
        result.put(byteBuffer);
        byteBuffer.clear();
        return result;
    }

    public static final ByteBuffer increaseBufferCapatity(final ByteBuffer byteBuffer, final int increaseSize) {
        if (byteBuffer == null) {
            throw new IllegalArgumentException("buffer is null");
        }

        if (increaseSize <= 0) {
            throw new IllegalArgumentException("increaseSize<=0");
        }

        final int capacity = byteBuffer.capacity() + increaseSize;
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity can't be negative");
        }
        final ByteBuffer result = byteBuffer.isDirect() ? ByteBuffer.allocateDirect(capacity) : ByteBuffer
                .allocate(capacity);
        result.order(byteBuffer.order());
        byteBuffer.flip();
        result.put(byteBuffer);
        byteBuffer.clear();
        return result;
    }


    public static void main(String[] args) throws IOException {
        File file = new File("C:/Users/Administrator/Desktop/aaa.txt");
        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rws");
        FileChannel fc = randomAccessFile.getChannel();
        ByteBuffer bb = ByteBuffer.allocateDirect(5);
        while (fc.read(bb) > 0) {
            if (!bb.hasRemaining()) {
                bb = FileUtils.increaseBufferCapatity(bb, 45);
            }
        }
        bb = FileUtils.decreaseBufferCapatity(bb, bb.position());

        bb.flip();
        byte[] temp = new byte[bb.capacity()];

        bb.get(temp, 0, temp.length);
        System.out.println(new String(temp));

        // System.out.println(bb.position());
        //System.out.println(new String(bb.array()));
    }

    private final int number;

    private final File file;

    private final AtomicInteger referenceCount = new AtomicInteger(0);

    protected FileChannel fc;

    protected RandomAccessFile raf;

    /**
     * 构造函数，会打开指定的文件，并且将指针指向文件结尾
     * 
     * @param file
     * @throws IOException
     */
    public FileUtils(final File file) throws IOException {
        this(file, 0, false);
    }

    public FileUtils(final File file, final int number) throws IOException {
        this(file, number, false);
    }

    int getNumber() {
        return this.number;
    }

    /**
     * 构造函数，会打开指定的文件，并且将指针指向文件结尾
     * 
     * @param file
     * @throws IOException
     */
    FileUtils(final File file, final int number, final boolean force) throws IOException {
        this.file = file;
        this.number = number;
        this.raf = new RandomAccessFile(file, force ? "rws" : "rw");
        this.fc = this.raf.getChannel();
        // 指针移到最后
        this.fc.position(this.fc.size());

    }

    /**
     * 获得文件的大小
     * 
     * @return 文件的大小
     * @throws IOException
     */
    public long getLength() throws IOException {
        return this.fc.size();
    }

    public void truncate(final long size) throws IOException {
        this.fc.truncate(size);
    }

    /**
     * 获取文件最后修改时间
     * 
     * @return
     * @throws IOException
     */
    public long lastModified() throws IOException {
        return this.file.lastModified();
    }

    /**
     * 删除文件
     * 
     * @return 是否删除成功
     * @throws IOException
     */
    public boolean delete() throws IOException {
        this.close();
        return this.file.delete();
    }

    /**
     * 强制将数据写回硬盘
     * 
     * @throws IOException
     */
    public void force() throws IOException {
        if (this.fc.isOpen()) {
            this.fc.force(false);
        }
    }

    /**
     * 关闭文件
     * 
     * @throws IOException
     */
    public void close() throws IOException {
        try {
            if (this.fc != null) {
                this.fc.close();
            }
        } finally {
            if (this.raf != null) {
                this.raf.close();
            }
        }
    }

    /**
     * 从文件读取数据到bf，直到读满或者读到文件结尾。 <br />
     * 文件的指针会向后移动bf的大小
     * 
     * @param bf
     * @throws IOException
     */
    public void read(final ByteBuffer bf) throws IOException {
        while (bf.hasRemaining()) {
            final int l = this.fc.read(bf);
            if (l < 0) {
                break;
            }
        }
    }

    /**
     * 从文件的制定位置读取数据到bf，直到读满或者读到文件结尾。 <br />
     * 文件指针不会移动
     * 
     * @param bf
     * @param offset
     * @throws IOException
     */
    public void read(final ByteBuffer bf, final long offset) throws IOException {
        int size = 0;
        while (bf.hasRemaining()) {
            final int l = this.fc.read(bf, offset + size);
            size += l;
            if (l < 0) {
                break;
            }
        }
    }

    /**
     * 写入bf长度的数据到文件，文件指针会向后移动
     * 
     * @param bf
     * @return 写入后的文件position
     * @throws IOException
     */
    public long write(final ByteBuffer bf) throws IOException {
        while (bf.hasRemaining()) {
            final int l = this.fc.write(bf);
            if (l < 0) {
                break;
            }
        }
        return this.fc.position();
    }

    /**
     * 从指定位置写入bf长度的数据到文件，文件指针<b>不会</b>向后移动
     * 
     * @param offset
     * @param bf
     * @throws IOException
     */
    public void write(final long offset, final ByteBuffer bf) throws IOException {
        int size = 0;
        while (bf.hasRemaining()) {
            final int l = this.fc.write(bf, offset + size);
            size += l;
            if (l < 0) {
                break;
            }
        }
    }

    /**
     * 对文件增加一个引用计数
     * 
     * @return 增加后的引用计数
     */
    public int increment() {
        return this.referenceCount.incrementAndGet();
    }

    /**
     * 设置文件引用
     * 
     * @param i
     */
    void setReferenceCount(final int i) {
        this.referenceCount.set(i);
    }

    /**
     * 对文件减少一个引用计数
     * 
     * @return 减少后的引用计数
     */
    int decrement() {
        return this.referenceCount.decrementAndGet();
    }

    /**
     * 文件是否还在使用（引用计数是否是0了）
     * 
     * @return 文件是否还在使用
     */
    public boolean isUnUsed() {
        return this.getReferenceCount() <= 0;
    }

    public long position() throws IOException {
        return this.fc.position();
    }

    /**
     * 获得引用计数的值
     * 
     * @return 引用计数的值
     */
    int getReferenceCount() {
        return this.referenceCount.get();
    }

    @Override
    public String toString() {
        String result = null;
        try {
            result = this.file.getName() + " , length = " + this.getLength() + " refCount = " + this.referenceCount
                    + " position:" + this.fc.position();
        } catch (final IOException e) {
            result = e.getMessage();
        }
        return result;
    }
}
