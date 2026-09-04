<?php
declare(strict_types=1);

ob_start();
require __DIR__ . '/bootstrap.php';

header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');

function mobile_json(array $data, int $status = 200): never {
    http_response_code($status);
    if (ob_get_level() > 0 && ob_get_length() !== false) @ob_clean();
    echo json_encode($data, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_INVALID_UTF8_SUBSTITUTE);
    exit;
}

function mobile_bearer(): string {
    $header = (string)($_SERVER['HTTP_AUTHORIZATION'] ?? '');
    if ($header === '' && function_exists('apache_request_headers')) {
        $headers = apache_request_headers();
        $header = (string)($headers['Authorization'] ?? $headers['authorization'] ?? '');
    }
    return preg_match('/^Bearer\s+(.+)$/i', trim($header), $m) ? trim($m[1]) : '';
}

function mobile_user(): array {
    $token = mobile_bearer();
    if ($token === '') mobile_json(['ok'=>false,'error'=>'Token requerido.'], 401);
    $stmt = db()->prepare(
        'SELECT u.id,u.name,u.email,u.role,u.phone,u.active,t.id token_id
         FROM mobile_api_tokens t
         JOIN users u ON u.id=t.user_id
         WHERE t.token_hash=? AND t.expires_at>NOW() AND u.active=1 LIMIT 1'
    );
    $stmt->execute([hash('sha256', $token)]);
    $user = $stmt->fetch();
    if (!$user) mobile_json(['ok'=>false,'error'=>'Sesión vencida o no válida.'], 401);
    db()->prepare('UPDATE mobile_api_tokens SET last_used_at=NOW() WHERE id=?')->execute([(int)$user['token_id']]);
    return $user;
}

function mobile_crm_allowed(array $user): void {
    if (($user['role'] ?? '') === 'technician') {
        mobile_json(['ok'=>false,'error'=>'El CRM todavía no está habilitado para técnicos.'], 403);
    }
    if (($user['role'] ?? '') === 'collaborator') {
        $stmt = db()->prepare('SELECT enabled FROM user_modules WHERE user_id=? AND module_key=? LIMIT 1');
        $stmt->execute([(int)$user['id'], 'whatsapp_crm']);
        $row = $stmt->fetch();
        if (!$row || (int)$row['enabled'] !== 1) {
            mobile_json(['ok'=>false,'error'=>'El módulo CRM no está habilitado para tu usuario.'], 403);
        }
    }
}

function mobile_scope(array $user): array {
    if (($user['role'] ?? '') === 'admin') return ['', []];
    return [' AND (a.assigned_user_id=? OR a.created_by=?)', [(int)$user['id'], (int)$user['id']]];
}

function mobile_select(): string {
    return 'SELECT a.id,a.client_id,a.service_description,a.assigned_user_id,
        a.scheduled_start,a.scheduled_end,a.price,a.status,a.address,a.city,a.notes,a.created_origin,
        c.name client_name,c.phone client_phone,u.name assigned_user_name
        FROM appointments a JOIN clients c ON c.id=a.client_id
        LEFT JOIN users u ON u.id=a.assigned_user_id';
}

function mobile_clean(array $row): array {
    $row['id'] = (int)$row['id'];
    $row['client_id'] = (int)$row['client_id'];
    $row['assigned_user_id'] = $row['assigned_user_id'] !== null ? (int)$row['assigned_user_id'] : null;
    $row['price'] = (float)$row['price'];
    return $row;
}

function mobile_payload(): array {
    $country = normalize_phone((string)($_POST['client_country_code'] ?? '57')) ?: '57';
    $phone = international_phone_value(trim((string)($_POST['client_phone'] ?? '')), $country);
    $normalized = normalize_phone($phone);
    $name = capitalize_first((string)($_POST['client_name'] ?? ''));
    $service = capitalize_first((string)($_POST['service_description'] ?? ''));
    $assigned = (int)($_POST['assigned_user_id'] ?? 0);
    $start = build_scheduled_start((string)($_POST['scheduled_date'] ?? ''), (string)($_POST['time_slot'] ?? ''));
    $end = $start ? date('Y-m-d H:i:s', strtotime($start . ' +1 hour')) : null;
    $price = (float)str_replace([',', '$', ' '], ['', '', ''], (string)($_POST['price'] ?? '0'));
    $address = capitalize_nullable((string)($_POST['address'] ?? ''));
    $city = capitalize_nullable((string)($_POST['city'] ?? ''));
    $notes = capitalize_nullable((string)($_POST['notes'] ?? ''));
    if (strlen($normalized) < 7 || $name === '' || $address === null || $service === '' || !$start || $assigned <= 0) {
        throw new RuntimeException('Teléfono, nombre, dirección, descripción, fecha, horario y colaborador son obligatorios.');
    }
    if (text_length($service) > 500) throw new RuntimeException('La descripción del servicio no puede superar 500 caracteres.');
    if ($price < 0) throw new RuntimeException('El valor del servicio no puede ser negativo.');
    $stmt = db()->prepare("SELECT id FROM users WHERE id=? AND role='collaborator' AND active=1 LIMIT 1");
    $stmt->execute([$assigned]);
    if (!$stmt->fetch()) throw new RuntimeException('Debes asignar un colaborador activo.');
    return compact('country','phone','normalized','name','service','assigned','start','end','price','address','city','notes');
}

function mobile_client(array $p, int $createdBy, ?int $fallbackClientId = null): array {
    $pdo = db();
    $client = client_by_normalized_phone($p['phone']);
    if (!$client && $fallbackClientId) {
        $stmt = $pdo->prepare('SELECT id,name,phone,address,city FROM clients WHERE id=? LIMIT 1');
        $stmt->execute([$fallbackClientId]);
        $client = $stmt->fetch() ?: null;
    }
    if ($client) {
        $id = (int)$client['id'];
        $address = $p['address'] ?: ($client['address'] ?? null);
        $city = $p['city'] ?: ($client['city'] ?? null);
        $stmt = $pdo->prepare('UPDATE clients SET name=?,phone=?,phone_normalized=?,address=?,city=? WHERE id=?');
        $stmt->execute([$p['name'],$p['phone'],$p['normalized'],$address,$city,$id]);
        return [$id,$address,$city];
    }
    $stmt = $pdo->prepare('INSERT INTO clients (name,phone,phone_normalized,address,city,created_by) VALUES (?,?,?,?,?,?)');
    $stmt->execute([$p['name'],$p['phone'],$p['normalized'],$p['address'],$p['city'],$createdBy]);
    return [(int)$pdo->lastInsertId(),$p['address'],$p['city']];
}

function mobile_check_conflict(int $assigned, string $start, int $exclude = 0): void {
    $sql = "SELECT a.id,c.name client_name FROM appointments a JOIN clients c ON c.id=a.client_id
            WHERE a.assigned_user_id=? AND a.status<>'cancelled' AND a.scheduled_start=?";
    $args = [$assigned,$start];
    if ($exclude > 0) { $sql .= ' AND a.id<>?'; $args[] = $exclude; }
    $sql .= ' LIMIT 1';
    $stmt = db()->prepare($sql);
    $stmt->execute($args);
    if ($row = $stmt->fetch()) {
        throw new RuntimeException('El colaborador ya tiene un servicio a esa hora con '.$row['client_name'].' (servicio #'.$row['id'].').');
    }
}

function mobile_create(array $user): array {
    if (($user['role'] ?? '') === 'technician') throw new RuntimeException('Los técnicos todavía no pueden crear servicios.');
    $p = mobile_payload();
    $pdo = db();
    $pdo->beginTransaction();
    try {
        [$clientId,$address,$city] = mobile_client($p,(int)$user['id']);
        mobile_check_conflict($p['assigned'],$p['start']);
        $origin = ($user['role'] ?? '') === 'collaborator' ? 'collaborator' : 'admin';
        $stmt = $pdo->prepare('INSERT INTO appointments
            (client_id,service_description,assigned_user_id,scheduled_start,scheduled_end,price,status,address,city,notes,created_by,created_origin)
            VALUES (?,?,?,?,?,? ,"assigned_unconfirmed",?,?,?,?,?)');
        $stmt->execute([$clientId,$p['service'],$p['assigned'],$p['start'],$p['end'],$p['price'],$address,$city,$p['notes'],(int)$user['id'],$origin]);
        $id = (int)$pdo->lastInsertId();
        $stmt = $pdo->prepare('INSERT INTO appointment_history (appointment_id,old_status,new_status,note,changed_by)
            VALUES (?,NULL,"assigned_unconfirmed",?,?)');
        $stmt->execute([$id,'Servicio creado desde la aplicación Android. Pendiente de aceptación por el colaborador.',(int)$user['id']]);
        $pdo->commit();
    } catch (Throwable $e) {
        if ($pdo->inTransaction()) $pdo->rollBack();
        throw $e;
    }
    $warning = null;
    if (notification_enabled('booking')) {
        $n = send_appointment_notification($id,'booking',date('Y-m-d H:i:s'));
        if (empty($n['ok'])) $warning = 'El servicio se guardó, pero no se pudo enviar la notificación de agendamiento.';
    }
    return ['ok'=>true,'appointment_id'=>$id,'message'=>'Servicio agendado.','warning'=>$warning];
}

function mobile_update(array $user): array {
    if (($user['role'] ?? '') !== 'admin') throw new RuntimeException('Solo el administrador puede editar servicios desde la aplicación.');
    $id = (int)($_POST['appointment_id'] ?? 0);
    if ($id <= 0) throw new RuntimeException('Servicio no válido.');
    $p = mobile_payload();
    $pdo = db();
    $pdo->beginTransaction();
    try {
        $stmt = $pdo->prepare('SELECT client_id,status,assigned_user_id FROM appointments WHERE id=? FOR UPDATE');
        $stmt->execute([$id]);
        $old = $stmt->fetch();
        if (!$old) throw new RuntimeException('El servicio no existe.');
        [$clientId,$address,$city] = mobile_client($p,(int)$user['id'],(int)$old['client_id']);
        mobile_check_conflict($p['assigned'],$p['start'],$id);
        $changed = (int)($old['assigned_user_id'] ?? 0) !== $p['assigned'];
        $status = $changed ? 'assigned_unconfirmed' : (string)$old['status'];
        $stmt = $pdo->prepare('UPDATE appointments SET client_id=?,service_description=?,assigned_user_id=?,scheduled_start=?,scheduled_end=?,price=?,status=?,address=?,city=?,notes=? WHERE id=?');
        $stmt->execute([$clientId,$p['service'],$p['assigned'],$p['start'],$p['end'],$p['price'],$status,$address,$city,$p['notes'],$id]);
        $note = $changed ? 'Servicio editado desde Android y reasignado. Queda en estado Creado hasta ser aceptado.' : 'Servicio editado desde la aplicación Android.';
        $stmt = $pdo->prepare('INSERT INTO appointment_history (appointment_id,old_status,new_status,note,changed_by) VALUES (?,?,?,?,?)');
        $stmt->execute([$id,(string)$old['status'],$status,$note,(int)$user['id']]);
        $pdo->commit();
    } catch (Throwable $e) {
        if ($pdo->inTransaction()) $pdo->rollBack();
        throw $e;
    }
    return ['ok'=>true,'appointment_id'=>$id,'message'=>'Servicio actualizado.'];
}

function mobile_chat_context(int $userId, string $jid, array $messages = []): array {
    $business = crm_chatwoot_client_context($userId,$jid,$messages);
    $name = '';
    $phone = (string)($business['phone'] ?? '');
    $avatar = '';
    if (function_exists('crm_chatwoot_conversation_detail')) {
        $detail = crm_chatwoot_conversation_detail($userId,$jid);
        if (!empty($detail['ok'])) {
            $sender = crm_chatwoot_sender_from_conversation($detail['conversation'] ?? []);
            $name = trim((string)($sender['name'] ?? ''));
            $raw = trim((string)($sender['phone'] ?? ''));
            if ($raw !== '') $phone = local_phone_value($raw);
            $avatar = trim((string)($sender['avatar_url'] ?? ''));
        }
    }
    if ($name === '' && !empty($business['client']['name'])) $name = (string)$business['client']['name'];
    if ($name === '') $name = $phone !== '' ? $phone : 'Contacto';
    return ['name'=>$name,'phone'=>$phone,'avatar'=>$avatar,'avatar_url'=>$avatar,
        'found'=>(bool)($business['found'] ?? false),'client'=>$business['client'] ?? null,'services'=>$business['services'] ?? []];
}

try {
    $action = trim((string)($_GET['action'] ?? $_POST['action'] ?? ''));

    if ($action === 'login') {
        if ($_SERVER['REQUEST_METHOD'] !== 'POST') mobile_json(['ok'=>false,'error'=>'Método no permitido.'],405);
        $email = strtolower(trim((string)($_POST['email'] ?? '')));
        $password = (string)($_POST['password'] ?? '');
        if ($email === '' || $password === '') mobile_json(['ok'=>false,'error'=>'Correo y contraseña son obligatorios.'],400);
        $stmt = db()->prepare('SELECT id,name,email,role,phone,password_hash FROM users WHERE email=? AND active=1 LIMIT 1');
        $stmt->execute([$email]);
        $user = $stmt->fetch();
        if (!$user || !password_verify($password,(string)$user['password_hash'])) mobile_json(['ok'=>false,'error'=>'Correo o contraseña incorrectos.'],401);
        db()->prepare('DELETE FROM mobile_api_tokens WHERE expires_at<=NOW()')->execute();
        $token = bin2hex(random_bytes(32));
        $expires = date('Y-m-d H:i:s', strtotime('+30 days'));
        $stmt = db()->prepare('INSERT INTO mobile_api_tokens (user_id,token_hash,device_name,expires_at) VALUES (?,?,?,?)');
        $stmt->execute([(int)$user['id'],hash('sha256',$token),'Android',$expires]);
        unset($user['password_hash']);
        $user['id'] = (int)$user['id'];
        mobile_json(['ok'=>true,'token'=>$token,'expires_at'=>$expires,'user'=>$user]);
    }

    $user = mobile_user();

    switch ($action) {
        case 'me':
            mobile_json(['ok'=>true,'user'=>['id'=>(int)$user['id'],'name'=>(string)$user['name'],'email'=>(string)$user['email'],'role'=>(string)$user['role']]]);
        case 'logout':
            db()->prepare('DELETE FROM mobile_api_tokens WHERE token_hash=?')->execute([hash('sha256',mobile_bearer())]);
            mobile_json(['ok'=>true]);
        case 'collaborators':
            $rows = db()->query("SELECT id,name FROM users WHERE role='collaborator' AND active=1 ORDER BY name")->fetchAll();
            foreach ($rows as &$r) $r['id'] = (int)$r['id'];
            mobile_json(['ok'=>true,'collaborators'=>$rows]);
        case 'today_services':
            $date = trim((string)($_GET['date'] ?? date('Y-m-d')));
            if (!preg_match('/^\d{4}-\d{2}-\d{2}$/',$date)) mobile_json(['ok'=>false,'error'=>'Fecha no válida.'],400);
            [$scope,$params] = mobile_scope($user);
            $stmt = db()->prepare(mobile_select().' WHERE DATE(a.scheduled_start)=?'.$scope.' ORDER BY a.scheduled_start,a.id');
            $stmt->execute(array_merge([$date],$params));
            mobile_json(['ok'=>true,'date'=>$date,'appointments'=>array_map('mobile_clean',$stmt->fetchAll())]);
        case 'calendar':
            $start = trim((string)($_GET['start'] ?? ''));
            $end = trim((string)($_GET['end'] ?? ''));
            if (!preg_match('/^\d{4}-\d{2}-\d{2}$/',$start) || !preg_match('/^\d{4}-\d{2}-\d{2}$/',$end)) mobile_json(['ok'=>false,'error'=>'Rango de fechas no válido.'],400);
            [$scope,$params] = mobile_scope($user);
            $next = date('Y-m-d',strtotime($end.' +1 day'));
            $stmt = db()->prepare(mobile_select().' WHERE a.scheduled_start>=? AND a.scheduled_start<?'.$scope.' ORDER BY a.scheduled_start,a.id');
            $stmt->execute(array_merge([$start.' 00:00:00',$next.' 00:00:00'],$params));
            mobile_json(['ok'=>true,'start'=>$start,'end'=>$end,'appointments'=>array_map('mobile_clean',$stmt->fetchAll())]);
        case 'appointment_detail':
            $id = (int)($_GET['id'] ?? 0);
            [$scope,$params] = mobile_scope($user);
            $stmt = db()->prepare(mobile_select().' WHERE a.id=?'.$scope.' LIMIT 1');
            $stmt->execute(array_merge([$id],$params));
            $a = $stmt->fetch();
            if (!$a) mobile_json(['ok'=>false,'error'=>'Servicio no encontrado.'],404);
            $h = db()->prepare('SELECT h.old_status,h.new_status,h.note,h.created_at,u.name changed_by_name FROM appointment_history h JOIN users u ON u.id=h.changed_by WHERE h.appointment_id=? ORDER BY h.created_at,h.id');
            $h->execute([$id]);
            mobile_json(['ok'=>true,'appointment'=>mobile_clean($a),'history'=>$h->fetchAll()]);
        case 'appointment_create':
            if ($_SERVER['REQUEST_METHOD'] !== 'POST') mobile_json(['ok'=>false,'error'=>'Método no permitido.'],405);
            mobile_json(mobile_create($user));
        case 'appointment_update':
            if ($_SERVER['REQUEST_METHOD'] !== 'POST') mobile_json(['ok'=>false,'error'=>'Método no permitido.'],405);
            mobile_json(mobile_update($user));
        case 'crm_chats':
            mobile_crm_allowed($user);
            $r = crm_chatwoot_find_chats((int)$user['id']);
            mobile_json($r,!empty($r['ok'])?200:400);
        case 'crm_messages':
            mobile_crm_allowed($user);
            $jid = trim((string)($_GET['jid'] ?? ''));
            $limit = max(1,min(50,(int)($_GET['limit'] ?? 30)));
            if ($jid === '') mobile_json(['ok'=>false,'error'=>'Conversación no válida.'],400);
            $r = crm_chatwoot_find_messages((int)$user['id'],$jid,$limit,0);
            if (!empty($r['ok'])) $r['context'] = mobile_chat_context((int)$user['id'],$jid,$r['messages'] ?? []);
            mobile_json($r,!empty($r['ok'])?200:400);
        case 'crm_send':
            mobile_crm_allowed($user);
            $jid = trim((string)($_POST['jid'] ?? ''));
            $message = trim((string)($_POST['message'] ?? ''));
            if ($jid === '' || $message === '') mobile_json(['ok'=>false,'error'=>'Conversación y mensaje son obligatorios.'],400);
            $r = crm_chatwoot_send_message((int)$user['id'],$jid,$message);
            if (!empty($r['ok'])) crm_autoresponder_takeover((int)$user['id'],$jid);
            mobile_json($r,!empty($r['ok'])?200:400);
        case 'crm_analyze':
            mobile_crm_allowed($user);
            $jid = trim((string)($_POST['jid'] ?? ''));
            if ($jid === '') mobile_json(['ok'=>false,'error'=>'Conversación no válida.'],400);
            $r = crm_analyze_conversation((int)$user['id'],$jid,[]);
            mobile_json($r,!empty($r['ok'])?200:400);
        case 'crm_mark_read':
            mobile_crm_allowed($user);
            $jid = trim((string)($_POST['jid'] ?? ''));
            if ($jid === '') mobile_json(['ok'=>false,'error'=>'Conversación no válida.'],400);
            $r = crm_chatwoot_mark_read((int)$user['id'],$jid);
            if (empty($r['ok'])) mobile_json(['ok'=>true,'marked'=>false,'warning'=>$r['error'] ?? 'No se pudo sincronizar leído.']);
            mobile_json($r);
        default:
            mobile_json(['ok'=>false,'error'=>'Acción no reconocida.'],404);
    }
} catch (Throwable $e) {
    error_log('mobile_api: '.$e->getMessage());
    mobile_json(['ok'=>false,'error'=>'No se pudo completar la operación.'],400);
}
