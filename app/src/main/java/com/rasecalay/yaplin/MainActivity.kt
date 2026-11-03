<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <!-- CONTENIDO PRINCIPAL -->
    <ScrollView
        android:id="@+id/layoutMainApp"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="#F5F6FA"
        android:fillViewport="true">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="20dp">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Yaplin"
                android:textSize="30sp"
                android:textStyle="bold"
                android:textColor="#5E17EB"
                android:layout_gravity="center_horizontal"
                android:layout_marginTop="8dp" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Asistente de Cobros en Tiempo Real"
                android:textSize="14sp"
                android:textColor="#666666"
                android:layout_gravity="center_horizontal"
                android:layout_marginBottom="16dp" />

            <!-- 1. Estado Principal -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:background="#FFFFFF"
                android:padding="14dp"
                android:elevation="3dp"
                android:gravity="center"
                android:layout_marginBottom="14dp">

                <TextView
                    android:id="@+id/tvStatusBadge"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="● ESTADO: ACTIVO"
                    android:textColor="#2E7D32"
                    android:textStyle="bold"
                    android:textSize="14sp"
                    android:background="#E8F5E9"
                    android:paddingLeft="12dp"
                    android:paddingRight="12dp"
                    android:paddingTop="6dp"
                    android:paddingBottom="6dp" />

                <TextView
                    android:id="@+id/tvStatusSub"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Escuchando pagos en segundo plano"
                    android:textColor="#555555"
                    android:textSize="13sp"
                    android:layout_marginTop="6dp" />
            </LinearLayout>

            <!-- 2. Servicios Monitoreados -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:background="#FFFFFF"
                android:padding="14dp"
                android:elevation="3dp"
                android:layout_marginBottom="14dp">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="SERVICIOS MONITOREADOS"
                    android:textSize="11sp"
                    android:textStyle="bold"
                    android:textColor="#777777"
                    android:layout_marginBottom="6dp" />

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:gravity="center_vertical">

                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="Yape (BCP) y Plin"
                        android:textColor="#222222"
                        android:textSize="14sp"
                        android:textStyle="bold" />

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="CONECTADO"
                        android:textColor="#1976D2"
                        android:textStyle="bold"
                        android:textSize="12sp"
                        android:background="#E3F2FD"
                        android:paddingLeft="8dp"
                        android:paddingRight="8dp"
                        android:paddingTop="4dp"
                        android:paddingBottom="4dp" />
                </LinearLayout>
            </LinearLayout>

            <!-- 3. Tarjeta de Último Pago Detectado -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:background="#FFFFFF"
                android:padding="16dp"
                android:elevation="3dp"
                android:layout_marginBottom="14dp">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="ÚLTIMO PAGO DETECTADO"
                    android:textSize="11sp"
                    android:textStyle="bold"
                    android:textColor="#777777" />

                <TextView
                    android:id="@+id/tvLastAmount"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="S/ --"
                    android:textSize="28sp"
                    android:textStyle="bold"
                    android:textColor="#2E7D32"
                    android:layout_marginTop="2dp" />

                <TextView
                    android:id="@+id/tvLastServiceDetail"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="A la espera de notificaciones..."
                    android:textColor="#888888"
                    android:textSize="13sp"
                    android:layout_marginTop="2dp" />
            </LinearLayout>

            <!-- 4. Tarjeta: Total de Hoy -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:background="#FFFFFF"
                android:padding="16dp"
                android:elevation="3dp"
                android:layout_marginBottom="16dp">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="TOTAL ACUMULADO HOY"
                    android:textSize="11sp"
                    android:textStyle="bold"
                    android:textColor="#777777" />

                <TextView
                    android:id="@+id/tvDailyAccumulated"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="S/ 0.00"
                    android:textSize="26sp"
                    android:textStyle="bold"
                    android:textColor="#2E7D32"
                    android:layout_marginTop="2dp
