export const equipmentTypes = ["NOTEBOOK", "MONITOR", "KEYBOARD", "MOUSE", "HEADSET", "OTHER"] as const;

export type EquipmentType = (typeof equipmentTypes)[number];
export type RequestStatus = "DRAFT" | "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";
export type ActorRole = "EMPLOYEE" | "APPROVER";

export type Actor = {
  userId: string;
  role: ActorRole;
  label: string;
};

export type EquipmentItemInput = {
  equipmentType: EquipmentType;
  quantity: number;
  specification?: string | null;
};

export type EquipmentRequestInput = {
  employeeName: string;
  employeeEmail: string;
  department: string;
  title: string;
  purpose: string;
  requiredDate: string;
  additionalNote?: string | null;
  items: EquipmentItemInput[];
};

export type EquipmentRequest = Omit<EquipmentRequestInput, "items"> & {
  id: string;
  requestNumber: string;
  status: RequestStatus;
  rejectionReason?: string | null;
  version: number;
  totalItems: number;
  createdAt: string;
  updatedAt: string;
  items: (EquipmentItemInput & { id: string })[];
};

export type ApiError = {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  path: string;
  fieldErrors: Record<string, string>;
};

