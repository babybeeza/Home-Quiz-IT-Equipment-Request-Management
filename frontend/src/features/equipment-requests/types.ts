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


export type RequestActionName = "submit" | "cancel" | "approve" | "reject";

export type ListSort = "createdAt,desc" | "createdAt,asc";

/** List parameters as held in the URL; empty strings mean "no filter". */
export type ListParams = {
  keyword: string;
  status: RequestStatus | "";
  department: string;
  page: number;
  sort: ListSort;
};

export type EquipmentRequestSummary = Pick<
  EquipmentRequest,
  "id" | "requestNumber" | "title" | "employeeName" | "department" | "requiredDate" | "totalItems" | "status" | "version" | "createdAt" | "updatedAt"
>;

export type EquipmentRequestPage = {
  content: EquipmentRequestSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};
