import Link from "next/link";

export default function RequestsPage() {
  return <main className="page-shell"><p className="eyebrow">IT EQUIPMENT REQUESTS</p><h1>คำขออุปกรณ์ IT</h1><p>สร้างและติดตามคำขออุปกรณ์ของคุณ รายการค้นหาแบบเต็มจะเพิ่มใน TASK-005</p><Link className="button" href="/requests/new">สร้างคำขอใหม่</Link></main>;
}
