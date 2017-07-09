package com.oymotion.gforcedev.adapters;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.oymotion.gforcedev.R;
import com.oymotion.gforcedev.gforce_service.gForceService;
import com.oymotion.gforcedev.gforce_service.gForceServices;
import com.oymotion.gforcedev.info_service.BleInfoService;
import com.oymotion.gforcedev.info_service.BleInfoServices;

import static java.lang.Boolean.TRUE;
import static java.lang.Boolean.FALSE;

/** Adapter for holding GATT services and characteristics discovered on
 *  the remote device.
 */
public class BleServicesAdapter extends BaseExpandableListAdapter {

	private final static String TAG = BleServicesAdapter.class.getSimpleName();

	public interface OnServiceItemClickListener {
		public void onDemoClick(BluetoothGattService service);

		public void onServiceEnabled(BluetoothGattService service,
				boolean enabled);

		public void onServiceUpdated(BluetoothGattService service);
	}

	private static final String MODE_READ = "R";
	private static final String MODE_NOTIFY = "N";
	private static final String MODE_WRITE = "W";
	private static final String MODE_INDICATE = "I";

	private final ArrayList<BluetoothGattService> services;
	private final HashMap<BluetoothGattService, ArrayList<BluetoothGattCharacteristic>> characteristics;
	private final LayoutInflater inflater;

	private OnServiceItemClickListener serviceListener;

	public BleServicesAdapter(Context context,
			List<BluetoothGattService> gattServices) {
		inflater = LayoutInflater.from(context);

		services = new ArrayList<BluetoothGattService>(gattServices.size());
		characteristics = new HashMap<BluetoothGattService, ArrayList<BluetoothGattCharacteristic>>(
				gattServices.size());
		for (BluetoothGattService gattService : gattServices) {
			final List<BluetoothGattCharacteristic> gattCharacteristics = gattService
					.getCharacteristics();
			characteristics.put(gattService,
					new ArrayList<BluetoothGattCharacteristic>(
							gattCharacteristics));
			services.add(gattService);
		}
	}

	public void setServiceListener(OnServiceItemClickListener listener) {
		this.serviceListener = listener;
	}

	@Override
	public int getGroupCount() {
		return services.size();
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return characteristics.get(getGroup(groupPosition)).size();
	}

	@Override
	public BluetoothGattService getGroup(int groupPosition) {
		return services.get(groupPosition);
	}

	/* If this adapter contains the Service Group, return true, else false. */
	public boolean containsGroup(String srvUuid) {
		int srvCount = getGroupCount();
		for (int i = 0; i < srvCount; i++) {
			if (srvUuid.equals(getGroup(i).getUuid().toString())) {
				return TRUE;
			}
		}
		return FALSE;
	}

	/* If this adapter contains the Service Characteristic Child, return true, else false. */
	public boolean containsChild(String charUuid) {
		int srvCount = getGroupCount();
		int charCount = 0;
		for (int i = 0; i < srvCount; i++) {
			charCount = getChildrenCount(i);
			for (int j = 0; j < charCount; j++) {
				if (charUuid.equals(getChild(i, j).getUuid().toString())) {
					return TRUE;
				}
			}
		}
		return FALSE;
	}

	@Override
	public BluetoothGattCharacteristic getChild(int groupPosition,
			int childPosition) {
		Log.d(TAG, "group:" + groupPosition + " child:" + childPosition);
		Log.d(TAG, "uuid:" + characteristics.get(getGroup(groupPosition))
								.get(childPosition).getUuid());
		return characteristics.get(getGroup(groupPosition)).get(childPosition);
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return groupPosition * 100 + childPosition;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded,
			View convertView, ViewGroup parent) {
		final GroupViewHolder holder;
		if (convertView == null) {
			holder = new GroupViewHolder();

			convertView = inflater.inflate(R.layout.listitem_service, parent,
					false);
			holder.name = (TextView) convertView.findViewById(R.id.name);
			holder.uuid = (TextView) convertView.findViewById(R.id.uuid);
			holder.demo = convertView.findViewById(R.id.demo);

			holder.demo.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (serviceListener == null)
						return;
					final BluetoothGattService service = (BluetoothGattService) holder.demo
							.getTag();
					serviceListener.onDemoClick(service);
				}
			});

			convertView.setTag(holder);
		} else {
			holder = (GroupViewHolder) convertView.getTag();
		}

		final BluetoothGattService item = getGroup(groupPosition);

		final String uuid = item.getUuid().toString();
		final String simpleUuid = "uuid: " + uuid.substring(4, 8);
		final BleInfoService infoService = BleInfoServices.getService(uuid);
		final gForceService gforceService = gForceServices.getService(uuid);

		final String serviceName;

		if (gforceService != null)
			serviceName = gforceService.getName();
		else if (infoService != null)
			serviceName = infoService.getName();
		else
			serviceName = "Unknown";

		holder.name.setText(serviceName);
		holder.uuid.setText(simpleUuid);
		holder.demo.setTag(item);
		holder.demo.setVisibility(View.GONE);
		return convertView;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
        Log.d(TAG, "getChildView!");
		final ChildViewHolder holder;
		if (convertView == null) {
			holder = new ChildViewHolder();

			convertView = inflater.inflate(R.layout.listitem_characteristic,
					parent, false);
			holder.name = (TextView) convertView.findViewById(R.id.name);
			holder.uuid = (TextView) convertView.findViewById(R.id.uuid);
			holder.modes = (TextView) convertView.findViewById(R.id.modes);
			holder.notes = (TextView) convertView.findViewById(R.id.notes);
			holder.seek = (SeekBar) convertView.findViewById(R.id.seek);
			holder.seek
					.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
						@Override
						public void onProgressChanged(SeekBar seekBar,
								int progress, boolean fromUser) {
							if (serviceListener == null || !fromUser)
								return;
						}

						@Override
						public void onStartTrackingTouch(SeekBar seekBar) {
						}

						@Override
						public void onStopTrackingTouch(SeekBar seekBar) {
						}
					});

			convertView.setTag(holder);
		} else {
			holder = (ChildViewHolder) convertView.getTag();
		}

		final BluetoothGattCharacteristic item = getChild(groupPosition,
				childPosition);

		final String uuid = item.getUuid().toString();
		final String simpleUuid = "uuid: " + uuid.substring(4, 8);
		final String name;
		final String modes = getModeString(item.getProperties());
		String notes = null;

		holder.service = item.getService();

		final String serviceUUID = item.getService().getUuid().toString();
		final BleInfoService infoService = BleInfoServices
				.getService(serviceUUID);
		final gForceService gforceService = gForceServices.getService(serviceUUID);



		if (gforceService != null) {
			name = gforceService.getCharacteristicName(uuid);
			notes = gforceService.getCharacteristicValue(uuid);
		} else if (infoService != null) {
			name = infoService.getCharacteristicName(uuid);
			notes = infoService.getCharacteristicValue(uuid);
		} else {
			name = "Unknown";
		}

		if ( notes != null) {
			holder.uuid.setVisibility(View.GONE);
			holder.seek.setVisibility(View.GONE);
			holder.notes.setVisibility(View.VISIBLE);
		} else {
			holder.uuid.setVisibility(View.VISIBLE);
			holder.seek.setVisibility(View.GONE);
			holder.notes.setVisibility(View.GONE);
		}

		holder.name.setText(name);
		holder.uuid.setText(simpleUuid);
		holder.modes.setText(modes);
		holder.notes.setText(notes);

		return convertView;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

	private static String getModeString(int prop) {
		final StringBuilder modeBuilder = new StringBuilder();
		if ((prop & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
			modeBuilder.append(MODE_READ);
		}
		if ((prop & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
			if (modeBuilder.length() > 0)
				modeBuilder.append("/");
			modeBuilder.append(MODE_NOTIFY);
		}
		if ((prop & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
			if (modeBuilder.length() > 0)
				modeBuilder.append("/");
			modeBuilder.append(MODE_WRITE);
		}
		if ((prop & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
			if (modeBuilder.length() > 0)
				modeBuilder.append("/");
			modeBuilder.append(MODE_INDICATE);
		}
		return modeBuilder.toString();
	}

	private static class GroupViewHolder {
		public TextView name;
		public TextView uuid;
		public View demo;
	}

	private static class ChildViewHolder {
		public BluetoothGattService service;

		public TextView name;
		public TextView uuid;
		public TextView modes;
		public TextView notes;
		public SeekBar seek;
	}

}
