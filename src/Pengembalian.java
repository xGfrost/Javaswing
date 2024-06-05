import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.swing.JOptionPane;
import java.util.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

/**
 *
 * @author vika
 */
public class Pengembalian extends javax.swing.JFrame {

     // Database connection details
    private static final String URL = "jdbc:mysql://localhost:3306/perpustakaan_pbo";
    private static final String USER = "root";
    private static final String PASSWORD = "";
    private static final int DENDA_PER_HARI = 3000;

    /**
     * Creates new form Pengembalian
     */
    public Pengembalian() {
        initComponents();
        setTitle("Pengembalian");
        // Add PropertyChangeListener to date_Kembali
        date_Kembali.getDateEditor().addPropertyChangeListener("date", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ("date".equals(evt.getPropertyName())) {
                    hitungDenda();
                }
            }
        });
    }
    
    private Connection getConnection() {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Koneksi berhasil!");
        } catch (SQLException e) {
            System.out.println("Koneksi gagal!");
            e.printStackTrace();
        }
        return connection;
    }

    public void cariDataPeminjam() {
        String idAnggota = txt_idAnggota.getText().trim();
        String idBuku = txt_idBuku.getText().trim();

        if (idAnggota.isEmpty() || idBuku.isEmpty()) {
            JOptionPane.showMessageDialog(null, "ID Anggota dan ID Buku tidak boleh kosong!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String query = "SELECT TANGGAL_PEMINJAM, TANGGAL_KEMBALI, JUMLAH_PINJAM FROM peminjam WHERE id_anggota = ? AND id_buku = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, idAnggota);
            pstmt.setString(2, idBuku);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String tanggalPinjam = rs.getString("TANGGAL_PEMINJAM");
                    String tanggalKembali = rs.getString("TANGGAL_KEMBALI");
                    int jumlahPinjam = rs.getInt("JUMLAH_PINJAM");

                    txt_tglPinjam.setText(tanggalPinjam);
                    txt_tglKembali.setText(tanggalKembali);
                    txt_jumlah.setText(String.valueOf(jumlahPinjam));
                } else {
                    JOptionPane.showMessageDialog(null, "Data Tidak Ditemukan", "Info", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Terjadi kesalahan saat mengakses database.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void hitungDenda() {
        String tglKembaliStr = txt_tglKembali.getText().trim();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date tglKembali;
        
        try {
            tglKembali = sdf.parse(tglKembaliStr);
        } catch (ParseException e) {
            return;
        }

        Date tglSekarang = date_Kembali.getDate();
        if (tglSekarang == null) {
            JOptionPane.showMessageDialog(null, "Tanggal kembali harus diisi!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        long diffInMillies = tglSekarang.getTime() - tglKembali.getTime();
        long diffInDays = diffInMillies / (1000 * 60 * 60 * 24);

        if (diffInDays <= 0) {
            txt_denda.setText("0");
            JOptionPane.showMessageDialog(null, "Tidak ada keterlambatan, tidak ada denda.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int jumlahBuku = Integer.parseInt(txt_jumlah.getText().trim());
        int denda = (int) diffInDays * jumlahBuku * DENDA_PER_HARI;
        txt_denda.setText(String.valueOf(denda));
    }
    
    public void prosesPengembalian() {
        String tglPengembalianStr = new SimpleDateFormat("yyyy-MM-dd").format(date_Kembali.getDate());
        String idBuku = txt_idBuku.getText(); // Ambil ID Buku dari suatu sumber, bisa dari JTextField atau JComboBox
        String idAnggota = txt_idAnggota.getText();
        
        if (tglPengembalianStr.isEmpty() || idBuku.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Tanggal pengembalian atau ID Buku tidak boleh kosong!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = getConnection()) {
            // Ambil nilai denda dari txt_denda
            int denda = Integer.parseInt(txt_denda.getText().trim());
            
            // Cek apakah buku sudah dikembalikan sebelumnya
        String checkPeminjamQuery = "SELECT TGL_KEMBALI_RILL FROM peminjam WHERE ID_ANGGOTA = ? AND ID_BUKU = ?";
        PreparedStatement pstmtCheckPeminjam = conn.prepareStatement(checkPeminjamQuery);
        pstmtCheckPeminjam.setString(1, idAnggota);
        pstmtCheckPeminjam.setString(2, idBuku);
        ResultSet rsCheckPeminjam = pstmtCheckPeminjam.executeQuery();

        if (rsCheckPeminjam.next()) {
            String tanggalPengembalian = rsCheckPeminjam.getString("TGL_KEMBALI_RILL");
            if (tanggalPengembalian != null && !tanggalPengembalian.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Buku dengan ID " + idBuku + " telah dikembalikan sebelumnya pada tanggal " + tanggalPengembalian, "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
        }
        

           // Update TANGGAL_PENGEMBALIAN dan DENDA di tabel peminjam
            String updatePeminjamQuery = "UPDATE peminjam SET TGL_KEMBALI_RILL = ?, DENDA = ? WHERE ID_ANGGOTA = ? AND ID_BUKU = ?";
            PreparedStatement pstmtPeminjam = conn.prepareStatement(updatePeminjamQuery);
            pstmtPeminjam.setString(1, tglPengembalianStr);
            pstmtPeminjam.setInt(2, denda);
            pstmtPeminjam.setString(3, idAnggota);
            pstmtPeminjam.setString(4, idBuku);
            int rowsUpdatedPeminjam = pstmtPeminjam.executeUpdate();

            // Update JUMLAH_BUKU_TERSEDIA di tabel buku
            int jumlahBukuDikembalikan = Integer.parseInt(txt_jumlah.getText().trim());
            String updateBukuQuery = "UPDATE buku SET JUMLAH_BUKU_TERSEDIA = JUMLAH_BUKU_TERSEDIA + ? WHERE ID_BUKU = ?";
            PreparedStatement pstmtBuku = conn.prepareStatement(updateBukuQuery);
            pstmtBuku.setInt(1, jumlahBukuDikembalikan);
            pstmtBuku.setString(2, idBuku);
            int rowsUpdatedBuku = pstmtBuku.executeUpdate();

            if (rowsUpdatedPeminjam > 0 && rowsUpdatedBuku > 0) {
                JOptionPane.showMessageDialog(null, "Proses pengembalian berhasil!", "Info", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "Gagal melakukan pengembalian!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Terjadi kesalahan saat mengakses database.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    
    public void clear() {
        txt_denda.setText("");
        txt_idAnggota.setText("");
        txt_idBuku.setText("");
        txt_jumlah.setText("");
        txt_tglKembali.setText("");
        txt_tglPinjam.setText("");
        date_Kembali.setDate(null); 
    }




    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        txt_idAnggota = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        txt_idBuku = new javax.swing.JTextField();
        txt_tglPinjam = new javax.swing.JTextField();
        txt_tglKembali = new javax.swing.JTextField();
        btn_search = new javax.swing.JButton();
        btn_kembalikan = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        txt_jumlah = new javax.swing.JTextField();
        txt_denda = new javax.swing.JTextField();
        date_Kembali = new com.toedter.calendar.JDateChooser();
        jLabel7 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setLocation(new java.awt.Point(325, 125));
        setUndecorated(true);

        txt_idAnggota.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txt_idAnggotaActionPerformed(evt);
            }
        });

        jLabel1.setText("ID Anggota");

        jLabel2.setText("ID Buku");

        jLabel3.setText("Tanggal Pinjam");

        jLabel4.setText("Tanggal Kembali");

        txt_tglPinjam.setEditable(false);
        txt_tglPinjam.setBackground(new java.awt.Color(204, 204, 204));

        txt_tglKembali.setEditable(false);
        txt_tglKembali.setBackground(new java.awt.Color(204, 204, 204));

        btn_search.setText("Search");
        btn_search.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_searchActionPerformed(evt);
            }
        });

        btn_kembalikan.setText("Kembalikan Buku");
        btn_kembalikan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_kembalikanActionPerformed(evt);
            }
        });

        jButton3.setText("Close");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jLabel5.setText("Jumlah");

        jLabel6.setText("Tanggal Pengembalian");

        txt_jumlah.setEditable(false);
        txt_jumlah.setBackground(new java.awt.Color(204, 204, 204));

        txt_denda.setEditable(false);
        txt_denda.setBackground(new java.awt.Color(204, 204, 204));
        txt_denda.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        txt_denda.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txt_dendaActionPerformed(evt);
            }
        });

        date_Kembali.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                date_KembaliMouseClicked(evt);
            }
        });
        date_Kembali.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                date_KembaliPropertyChange(evt);
            }
        });
        date_Kembali.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                date_KembaliKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                date_KembaliKeyReleased(evt);
            }
        });

        jLabel7.setText("Denda");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(99, 99, 99)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel3)
                    .addComponent(jLabel2)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5)
                    .addComponent(jLabel6)
                    .addComponent(jLabel7))
                .addGap(72, 72, 72)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButton3)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btn_kembalikan)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(date_Kembali, javax.swing.GroupLayout.PREFERRED_SIZE, 256, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                        .addComponent(txt_jumlah, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 256, Short.MAX_VALUE)
                                        .addComponent(txt_idAnggota, javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(txt_idBuku, javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(txt_tglPinjam, javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(txt_tglKembali, javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(txt_denda)))
                                .addGap(51, 51, 51)
                                .addComponent(btn_search)))
                        .addContainerGap(145, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txt_idAnggota, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(txt_idBuku, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn_search))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(txt_tglPinjam, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(txt_tglKembali, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(txt_jumlah, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel6)
                    .addComponent(date_Kembali, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(txt_denda, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(30, 30, 30)
                .addComponent(btn_kembalikan)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton3)
                .addContainerGap(45, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        home obj = new home();
        obj.setVisible(true);
        dispose();      
    }//GEN-LAST:event_jButton3ActionPerformed

    private void btn_kembalikanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_kembalikanActionPerformed
        prosesPengembalian();
    }//GEN-LAST:event_btn_kembalikanActionPerformed

    private void btn_searchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_searchActionPerformed
        cariDataPeminjam();
    }//GEN-LAST:event_btn_searchActionPerformed

    private void txt_idAnggotaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txt_idAnggotaActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txt_idAnggotaActionPerformed

    private void date_KembaliKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_date_KembaliKeyPressed
      
    }//GEN-LAST:event_date_KembaliKeyPressed

    private void date_KembaliPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_date_KembaliPropertyChange
        hitungDenda();
    }//GEN-LAST:event_date_KembaliPropertyChange

    private void date_KembaliKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_date_KembaliKeyReleased
       
    }//GEN-LAST:event_date_KembaliKeyReleased

    private void date_KembaliMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_date_KembaliMouseClicked
       
    }//GEN-LAST:event_date_KembaliMouseClicked

    private void txt_dendaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txt_dendaActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txt_dendaActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Pengembalian.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Pengembalian.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Pengembalian.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Pengembalian.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Pengembalian().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_kembalikan;
    private javax.swing.JButton btn_search;
    private com.toedter.calendar.JDateChooser date_Kembali;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JTextField txt_denda;
    private javax.swing.JTextField txt_idAnggota;
    private javax.swing.JTextField txt_idBuku;
    private javax.swing.JTextField txt_jumlah;
    private javax.swing.JTextField txt_tglKembali;
    private javax.swing.JTextField txt_tglPinjam;
    // End of variables declaration//GEN-END:variables
}
